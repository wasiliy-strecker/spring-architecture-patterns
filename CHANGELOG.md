# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added

- Spring Boot and Spring Modulith application foundation
- Explicit return-workflow module boundaries
- Automated module, layering, and application-context verification
- Java 21 and Java 25 continuous integration
- Framework-independent return intake domain and application service
- Public intake facade and immutable `ReturnRequested` event contract
- PostgreSQL persistence with separate JPA entities and Flyway constraints
- Testcontainers coverage for persistence, duplicates, events, and rollback
- Inspection aggregate with a single completion transition and optimistic locking
- Deterministic approval and rejection policy with stable module event contracts
- Durable Spring Modulith JDBC event publications and idempotent PostgreSQL consumers
- Isolated module scenarios and an end-to-end asynchronous workflow test
- Durable refund scheduling and idempotent provider settlement acknowledgement
- Monotonic CQRS return-case projection with a processed-event ledger
- Reverse-order event scenarios and full workflow-to-read-model integration coverage
- Scope-protected OAuth 2.0 REST adapters for the complete return workflow
- RFC 9457 problem responses, OAuth resource metadata, and request correlation
- Health probes, Prometheus event-publication metrics, and protected diagnostics
- Explicit bounded resubmission of incomplete events with operator audit logging
- Layered, non-root OCI packaging and a hardened local Compose topology
- Executable HTTP end-to-end workflow using an ephemeral RSA signing key
- Continuous container verification and automated Docker base-image updates
- Flyway currency-column alignment with production schema validation in
  PostgreSQL integration tests
- Offline RSA JWT decoding with explicit issuer, audience, signature, and time
  validation
