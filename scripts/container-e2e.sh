#!/usr/bin/env bash

set -Eeuo pipefail
IFS=$'\n\t'

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly repository_root
readonly compose_file="${repository_root}/compose.yaml"
readonly project_name="${COMPOSE_PROJECT_NAME:-returns-e2e-${RANDOM}-${RANDOM}}"
readonly app_port="${APP_PORT:-18080}"
readonly base_url="http://127.0.0.1:${app_port}"

fail() {
  printf 'ERROR: %s\n' "$*" >&2
  return 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command is unavailable: $1"
}

for required_command in curl docker jq openssl; do
  require_command "${required_command}"
done

docker compose version >/dev/null
docker info >/dev/null

if [[ ! "${app_port}" =~ ^[0-9]+$ ]] || ((app_port < 1 || app_port > 65535)); then
  fail "APP_PORT must be an integer between 1 and 65535"
fi

temporary_directory="$(mktemp -d)"
readonly temporary_directory
readonly private_key="${temporary_directory}/jwt-private.pem"
readonly public_key="${temporary_directory}/jwt-public.pem"
readonly -a compose=(docker compose --project-name "${project_name}" --file "${compose_file}")

cleanup() {
  local exit_code=$?
  trap - EXIT INT TERM

  if ((exit_code != 0)); then
    "${compose[@]}" ps >&2 || true
    "${compose[@]}" logs --no-color --tail=200 >&2 || true
  fi

  "${compose[@]}" down --volumes --remove-orphans >/dev/null 2>&1 || true
  rm -rf "${temporary_directory}"
  exit "${exit_code}"
}
trap cleanup EXIT INT TERM

umask 077
openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out "${private_key}" >/dev/null 2>&1
openssl pkey -in "${private_key}" -pubout -out "${public_key}" >/dev/null 2>&1

base64_urlencode() {
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

issue_token() {
  local scopes=$1
  local issued_at expires_at header payload unsigned signature
  issued_at="$(date +%s)"
  expires_at="$((issued_at + 600))"
  header='{"alg":"RS256","typ":"JWT"}'
  payload="$(
    printf \
      '{"iss":"https://auth.example.test","sub":"container-e2e","aud":"returns-api","iat":%s,"exp":%s,"scope":"%s"}' \
      "${issued_at}" \
      "${expires_at}" \
      "${scopes}"
  )"
  unsigned="$(printf '%s' "${header}" | base64_urlencode).$(printf '%s' "${payload}" | base64_urlencode)"
  signature="$(
    printf '%s' "${unsigned}" \
      | openssl dgst -sha256 -sign "${private_key}" \
      | base64_urlencode
  )"
  printf '%s.%s' "${unsigned}" "${signature}"
}

workflow_token="$(
  issue_token \
    'returns:read returns:write returns:inspect refunds:settle operations:read operations:manage'
)"
readonly workflow_token
read_only_token="$(issue_token 'returns:read')"
readonly read_only_token

export APP_PORT="${app_port}"
DATABASE_PASSWORD="$(openssl rand -hex 24)"
export DATABASE_PASSWORD
export IMAGE_REVISION="${GITHUB_SHA:-local}"
export JWT_PUBLIC_KEY_FILE="${public_key}"

"${compose[@]}" config --quiet
"${compose[@]}" up \
  --build \
  --detach \
  --wait \
  --wait-timeout "${E2E_START_TIMEOUT_SECONDS:-180}"

app_container="$("${compose[@]}" ps --quiet app)"
readonly app_container
configured_user="$(docker inspect --format '{{.Config.User}}' "${app_container}")"
readonly configured_user
read_only_root="$(docker inspect --format '{{.HostConfig.ReadonlyRootfs}}' "${app_container}")"
readonly read_only_root

[[ "${configured_user}" == "10001:10001" ]] \
  || fail "Application container must run as 10001:10001, got ${configured_user}"
[[ "${read_only_root}" == "true" ]] \
  || fail "Application container root filesystem must be read-only"

request_status() {
  local output_file=$1
  shift
  curl \
    --silent \
    --show-error \
    --connect-timeout 5 \
    --max-time 15 \
    --output "${output_file}" \
    --write-out '%{http_code}' \
    "$@"
}

expect_status() {
  local actual=$1
  local expected=$2
  local operation=$3
  local response_file=$4

  if [[ "${actual}" != "${expected}" ]]; then
    printf '%s returned HTTP %s; expected %s\n' "${operation}" "${actual}" "${expected}" >&2
    cat "${response_file}" >&2
    return 1
  fi
}

readonly unknown_return_id='00000000-0000-0000-0000-000000000000'
readonly unauthorized_response="${temporary_directory}/unauthorized.json"
unauthorized_status="$(
  request_status \
    "${unauthorized_response}" \
    "${base_url}/api/v1/returns/${unknown_return_id}"
)"
expect_status "${unauthorized_status}" 401 "Unauthenticated query" "${unauthorized_response}"
jq --exit-status '.code == "AUTHENTICATION_REQUIRED"' "${unauthorized_response}" >/dev/null

readonly create_payload='{"orderReference":"ORDER-CONTAINER-1001","itemReference":"LINE-7","reason":"DAMAGED","comment":"Container end-to-end example.","requestedRefundMinorUnits":18900,"currency":"EUR"}'
readonly forbidden_response="${temporary_directory}/forbidden.json"
forbidden_status="$(
  request_status \
    "${forbidden_response}" \
    --request POST \
    --header "Authorization: Bearer ${read_only_token}" \
    --header 'Content-Type: application/json' \
    --data "${create_payload}" \
    "${base_url}/api/v1/returns"
)"
expect_status "${forbidden_status}" 403 "Insufficient-scope command" "${forbidden_response}"
jq --exit-status '.code == "INSUFFICIENT_SCOPE"' "${forbidden_response}" >/dev/null

readonly create_response="${temporary_directory}/create-return.json"
create_status="$(
  request_status \
    "${create_response}" \
    --request POST \
    --header "Authorization: Bearer ${workflow_token}" \
    --header 'Content-Type: application/json' \
    --header 'X-Request-Id: container-e2e-create' \
    --data "${create_payload}" \
    "${base_url}/api/v1/returns"
)"
expect_status "${create_status}" 201 "Return creation" "${create_response}"
return_id="$(jq --exit-status --raw-output '.returnId' "${create_response}")"
readonly return_id
[[ "${return_id}" =~ ^[0-9a-f-]{36}$ ]] || fail "Return API produced an invalid identifier"

readonly inspection_payload='{"outcome":"ACCEPTED","note":"Damage confirmed in container E2E."}'
readonly inspection_response="${temporary_directory}/inspection.json"
inspection_completed=false
for ((attempt = 1; attempt <= 30; attempt++)); do
  inspection_status="$(
    request_status \
      "${inspection_response}" \
      --request POST \
      --header "Authorization: Bearer ${workflow_token}" \
      --header 'Content-Type: application/json' \
      --data "${inspection_payload}" \
      "${base_url}/api/v1/returns/${return_id}/inspection"
  )"

  if [[ "${inspection_status}" == "200" ]]; then
    inspection_completed=true
    break
  fi
  if [[ "${inspection_status}" != "404" ]]; then
    expect_status \
      "${inspection_status}" \
      200 \
      "Inspection completion" \
      "${inspection_response}"
  fi
  sleep 1
done
[[ "${inspection_completed}" == "true" ]] \
  || fail "Inspection work was not registered within 30 seconds"
jq --exit-status '.outcome == "ACCEPTED"' "${inspection_response}" >/dev/null

readonly case_response="${temporary_directory}/return-case.json"
wait_for_case() {
  local description=$1
  local expression=$2
  local attempt query_status

  for ((attempt = 1; attempt <= 40; attempt++)); do
    query_status="$(
      request_status \
        "${case_response}" \
        --header "Authorization: Bearer ${workflow_token}" \
        "${base_url}/api/v1/returns/${return_id}"
    )"
    if [[ "${query_status}" == "200" ]] \
      && jq --exit-status "${expression}" "${case_response}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  printf 'Timed out waiting for %s\n' "${description}" >&2
  cat "${case_response}" >&2
  return 1
}

wait_for_case \
  'the scheduled refund projection' \
  '.status == "REFUND_SCHEDULED" and .refundStatus == "SCHEDULED"'

readonly refund_response="${temporary_directory}/refund.json"
refund_status="$(
  request_status \
    "${refund_response}" \
    --request PUT \
    --header "Authorization: Bearer ${workflow_token}" \
    --header 'Content-Type: application/json' \
    --data '{"providerReference":"PSP-CONTAINER-1001"}' \
    "${base_url}/api/v1/returns/${return_id}/refund"
)"
expect_status "${refund_status}" 200 "Refund settlement" "${refund_response}"
jq --exit-status '.status == "COMPLETED"' "${refund_response}" >/dev/null

wait_for_case \
  'the completed refund projection' \
  '.status == "REFUNDED"
    and .refundStatus == "COMPLETED"
    and .requestedRefundMinorUnits == 18900
    and .currency == "EUR"'

readonly metrics_response="${temporary_directory}/prometheus.txt"
metrics_status="$(
  request_status \
    "${metrics_response}" \
    --header "Authorization: Bearer ${workflow_token}" \
    "${base_url}/actuator/prometheus"
)"
expect_status "${metrics_status}" 200 "Prometheus scrape" "${metrics_response}"
grep --quiet '^returns_event_publications_incomplete' "${metrics_response}" \
  || fail "Event-publication metric is missing"

printf 'Container end-to-end workflow passed.\n'
printf '  Return: %s\n' "${return_id}"
printf '  Final stage: %s\n' "$(jq --raw-output '.status' "${case_response}")"
printf '  Runtime user: %s\n' "${configured_user}"
