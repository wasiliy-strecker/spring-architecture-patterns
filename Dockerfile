# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress clean package -DskipTests -DskipITs \
    && java -Djarmode=tools -jar target/application.jar \
        extract --layers --destination target/extracted

FROM eclipse-temurin:21-jre-alpine AS runtime

ARG OCI_VERSION=0.1.0-SNAPSHOT
ARG OCI_REVISION=local

LABEL org.opencontainers.image.title="Spring Architecture Patterns" \
      org.opencontainers.image.description="Modular returns and refunds reference application" \
      org.opencontainers.image.source="https://github.com/wasiliy-strecker/spring-architecture-patterns" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.version="${OCI_VERSION}" \
      org.opencontainers.image.revision="${OCI_REVISION}"

RUN addgroup -g 10001 -S application \
    && adduser -u 10001 -S -D -H -G application application

WORKDIR /application

COPY --from=build --chown=10001:10001 /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=10001:10001 /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=10001:10001 /workspace/target/extracted/application/ ./

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1:8080/readyz || exit 1

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "application.jar"]
