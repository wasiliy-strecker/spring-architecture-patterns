# Security Policy

## Supported versions

Until the first stable release, security fixes are applied to the latest commit
on `main`.

## Reporting a vulnerability

Please use GitHub's private vulnerability reporting for this repository. Do not
open a public issue containing exploit details, credentials, or customer data.

Reports should describe the affected component, reproduction steps, impact, and
any suggested mitigation. Receipt will be acknowledged as soon as practical.

The container end-to-end script creates a short-lived RSA private key below a
restricted temporary directory and deletes it during cleanup. That key is test
material only. Never reuse it, persist it, or add any private signing key to the
repository.
