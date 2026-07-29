# Spring Architecture Patterns

[![Verify](https://github.com/wasiliy-strecker/spring-architecture-patterns/actions/workflows/verify.yml/badge.svg)](https://github.com/wasiliy-strecker/spring-architecture-patterns/actions/workflows/verify.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 4.1](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)

A production-minded Spring Boot reference application that demonstrates
architecture patterns through one coherent returns and refunds workflow.

The project is intentionally a modular monolith: business capabilities are
independently testable and protected by executable boundaries while deployment
and operational complexity stay low.

> **Current milestone — durable inspection and resolution:** return intake now
> starts an asynchronous inspection. Completing that inspection once triggers a
> deterministic approval or rejection, with every listener delivery tracked in
> PostgreSQL. Refunds and query projections remain explicit roadmap items rather
> than placeholder implementations.

## What this project demonstrates

- business-aligned modules rather than technical package silos
- Spring Modulith verification for cycles, exposed APIs, and allowed dependencies
- hexagonal boundaries inside each module
- framework-independent domain and application layers enforced with ArchUnit
- transaction demarcation through an application port rather than framework annotations
- PostgreSQL persistence owned by a JPA adapter and schema managed by Flyway
- transactional module events with Spring Modulith's JDBC publication registry
- idempotent consumers backed by atomic PostgreSQL conflict handling
- aggregate invariants plus optimistic locking for concurrent completion attempts
- isolated module scenarios that verify collaboration through public events
- deterministic unit tests plus real PostgreSQL integration tests with Testcontainers
- generated module diagrams that cannot silently drift from the code
- Java 21 baseline with Java 21 and Java 25 CI verification

## Business workflow

```mermaid
flowchart LR
    A[Return requested] --> B[Item inspected]
    B -->|accepted| C[Return approved]
    B -->|rejected| D[Return rejected]
    C --> E[Refund scheduled]
    A -. read model .-> Q[Return case query]
    B -. projection .-> Q
    C -. projection .-> Q
    D -. projection .-> Q
    E -. projection .-> Q
```

Return intake, warehouse inspection, and deterministic resolution are
implemented. The remaining reference flow covers refund scheduling and an
eventually consistent query view. The repository does not claim those later
features until their milestones are merged.

## Implemented use cases

```java
ReturnReceipt receipt =
    returnIntake.request(
        new RequestReturnCommand(
            "ORDER-1001",
            "LINE-2",
            "DAMAGED",
            "Outer packaging was crushed.",
            12_500,
            "EUR"));

InspectionReceipt inspection =
    inspectionWork.complete(
        new CompleteInspectionCommand(
            receipt.returnId(),
            "ACCEPTED",
            "Damage confirmed by warehouse."));
```

The application normalizes and validates the request, rejects a duplicate
order-item pair, inserts it within a transaction, and publishes
`ReturnRequested`. The event exposes only identifiers, scalar values, and a
timestamp—not a JPA entity or internal aggregate.

The inspection listener registers pending work asynchronously. The aggregate
allows one completion transition, and JPA optimistic locking resolves competing
writes. `ACCEPTED` produces a full-refund `ReturnApproved`; `REJECTED` produces
`ReturnRejected` with the stable reason `INSPECTION_FAILED`.

```mermaid
sequenceDiagram
    participant I as intake
    participant R as event_publication
    participant N as inspection
    participant D as resolution
    I->>R: ReturnRequested + listener publication
    R-->>N: transactional delivery
    N->>R: InspectionCompleted + listener publication
    R-->>D: transactional delivery
    alt ACCEPTED
        D-->>D: ReturnApproved
    else REJECTED
        D-->>D: ReturnRejected
    end
```

## Module map

| Module | Responsibility | Intended public collaboration |
|---|---|---|
| `intake` | Capture and validate a return request | API and `ReturnRequested` event |
| `inspection` | Record the physical inspection once | API and `InspectionCompleted` event |
| `resolution` | Decide approval or rejection | Resolution events |
| `refund` | Schedule an approved refund | API and refund event |
| `query` | Build read-optimized return case views | Query API |

Every module owns its domain, use cases, and adapters. Other modules may use only
explicitly exposed APIs or named event interfaces.

## Architecture guardrails

```mermaid
flowchart LR
    WEB[Inbound adapters] --> APP[Application]
    DB[Outbound adapters] --> APP
    APP --> DOMAIN[Domain]
    DOMAIN -. no framework dependencies .-> DOMAIN
```

`ApplicationModulesTest` verifies the Spring Modulith model and generates
PlantUML plus module canvases under `target/spring-modulith-docs`.
`ArchitectureRulesTest` prevents domain and application code from depending on
Spring, Jakarta Persistence, or servlet APIs and enforces inward dependency
direction.

See [the architecture notes](docs/architecture.md) for the decisions and
planned dependency direction.

## Two-minute review

1. Start with the
   [public intake facade](src/main/java/io/github/wasiliystrecker/returns/intake/ReturnIntake.java).
2. Follow `ReturnRequested` into the idempotent
   [inspection listener](src/main/java/io/github/wasiliystrecker/returns/inspection/adapter/ReturnRequestedListener.java).
3. Review the one-way state transition in
   [InspectionCase](src/main/java/io/github/wasiliystrecker/returns/inspection/domain/InspectionCase.java)
   and its
   [completion use case](src/main/java/io/github/wasiliystrecker/returns/inspection/application/CompleteInspectionService.java).
4. Inspect the deterministic
   [resolution policy](src/main/java/io/github/wasiliystrecker/returns/resolution/domain/ResolutionPolicy.java)
   and atomic persistence adapters.
5. Finish with the
   [cross-module PostgreSQL test](src/test/java/io/github/wasiliystrecker/returns/ReturnWorkflowIT.java)
   and [delivery semantics](docs/architecture.md#delivery-semantics).

## Build

Requirements: a full JDK from version 21 through 25 and Docker for the
PostgreSQL integration test. The Maven Wrapper downloads the pinned Maven
distribution.

```bash
./mvnw clean verify
```

Run only the fast architecture checks:

```bash
./mvnw test -Dtest=ApplicationModulesTest,ArchitectureRulesTest
```

Run all unit and architecture checks without integration tests:

```bash
./mvnw clean verify -DskipITs
```

Start PostgreSQL for the application:

```bash
docker run --rm --name returns-postgres \
  -e POSTGRES_DB=returns \
  -e POSTGRES_USER=returns \
  -e POSTGRES_PASSWORD=returns \
  -p 5432:5432 \
  postgres:18.3-alpine
```

Then run the application in a second terminal:

```bash
./mvnw spring-boot:run
```

Flyway creates the schema and Hibernate validates that its mapping matches it.

## Technology choices

- Java 21
- Spring Boot 4.1
- Spring Modulith 2.1
- PostgreSQL 18 and Flyway
- Spring Data JPA
- Testcontainers 2.0
- JUnit and AssertJ
- ArchUnit
- Maven Wrapper and Spotless
- GitHub Actions

## Roadmap

- [x] Executable modular-monolith foundation
- [x] Return intake and PostgreSQL persistence
- [x] Inspection and resolution modules
- [ ] Reliable refund handling and query projections
- [ ] Secured REST API and operational insight
- [ ] Container packaging, end-to-end example, and first release

Design choices are recorded in [docs/architecture.md](docs/architecture.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
