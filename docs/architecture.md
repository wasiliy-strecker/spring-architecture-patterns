# Architecture

## Decision

The return and refund workflow is implemented as a functionally modular
monolith. One deployable unit is appropriate for the current scale, while
business modules and executable dependency rules preserve options for later
extraction.

This repository uses patterns only where they solve a visible problem. It does
not split a small workflow into networked services merely to demonstrate
microservices.

## Business modules

```mermaid
flowchart LR
    I[intake] -->|ReturnRequested| N[inspection]
    N -->|InspectionCompleted| R[resolution]
    R -->|ReturnApproved| F[refund]
    I -->|events| Q[query]
    N -->|events| Q
    R -->|events| Q
    F -->|events| Q
```

The solid path from `intake` through `inspection` to `resolution` is
implemented. `refund` and `query` remain deliberately independent until their
behavior is implemented.

## Internal dependency rule

Each business module follows this direction:

```text
adapter -> application -> domain
```

- `domain` contains state, value objects, and business invariants.
- `application` coordinates use cases and declares outbound ports.
- `adapter` translates HTTP or persistence concerns at the boundary.

Domain and application packages cannot depend on Spring, Jakarta Persistence,
or servlet APIs. The domain cannot depend on application or adapter packages,
and the application cannot depend on adapters. ArchUnit enforces both rules.

## Return intake slice

```mermaid
flowchart LR
    API[ReturnIntake facade] --> UC[RequestReturnService]
    UC --> D[ReturnRequest + Money]
    UC --> RP[ReturnRequestRepository port]
    UC --> TX[TransactionRunner port]
    UC --> EP[DomainEventPublisher port]
    JPA[JPA adapter] --> RP
    TM[Spring transaction adapter] --> TX
    SPRING[Spring event adapter] --> EP
    JPA --> PG[(PostgreSQL)]
    F[Flyway V1] --> PG
```

`RequestReturnService` is constructed explicitly in Spring configuration but
contains no Spring annotation. A `TransactionRunner` port keeps transaction
semantics visible without coupling the use case to a framework. The JPA model
is separate from the immutable domain record, and Flyway repeats critical
invariants as database constraints.

## Inspection and resolution slices

```mermaid
flowchart LR
    RR[ReturnRequested] --> RL[ReturnRequestedListener]
    RL --> RI[RegisterInspectionService]
    RI --> IC[(inspection_case)]
    API[InspectionWork] --> CI[CompleteInspectionService]
    CI --> AGG[InspectionCase]
    CI --> IC
    CI --> EC[InspectionCompleted]
    EC --> IL[InspectionCompletedListener]
    IL --> RS[ResolveInspectionService]
    RS --> RP[ResolutionPolicy]
    RS --> RD[(return_resolution)]
    RS --> OK[ReturnApproved]
    RS --> NO[ReturnRejected]
```

`InspectionCase` is the authority for the one-way `PENDING → COMPLETED`
transition. The persistence adapter carries its version through JPA's
optimistic-lock field so two operators cannot both complete the same work.
Event redelivery is a different concern: registration and resolution use
PostgreSQL `INSERT … ON CONFLICT DO NOTHING`, making the idempotency decision
atomic instead of relying on a race-prone read-before-write check.

`ResolutionPolicy` is a pure, deterministic domain service. It maps
`ACCEPTED` to a full refund approval and `REJECTED` to the machine-readable
reason `INSPECTION_FAILED`. It knows nothing about events, Spring, or
persistence.

## Cross-module contracts

Synchronous collaboration uses a small public facade in the module base package.
Asynchronous collaboration uses immutable events exposed through a named
`events` interface. Event payloads contain identifiers, primitives, and
timestamps rather than persistence entities or internal domain objects.

Spring Modulith verifies:

- no cycles between modules
- no access to another module's internals
- only explicitly allowed dependencies once collaboration is introduced

## Architectural test strategy

`ApplicationModulesTest` treats the discovered Spring Modulith model as an
executable specification. It verifies boundaries and produces diagrams and
module canvases from the compiled code.

`ArchitectureRulesTest` protects the framework-independent core.
`ReturnIntakePersistenceIT` starts the complete application against PostgreSQL,
exercises Flyway and Hibernate validation, checks the stored row and event, and
proves that a failing synchronous event listener rolls back persistence.
`InspectionModuleIT` and `ResolutionModuleIT` use Spring Modulith's `Scenario`
API to test each module only through exposed events and APIs.
`ReturnWorkflowIT` verifies the complete asynchronous path, publication
completion, optimistic version increment, and duplicate event delivery against
real PostgreSQL.

The generated documents live below `target/spring-modulith-docs` and are not
versioned; the source of truth remains the code and its verification tests.

## Delivery semantics

Business events are published inside the originating transaction. Spring
Modulith writes one `event_publication` row per transactional listener in that
same transaction, then invokes each `@ApplicationModuleListener` asynchronously
in a new transaction. A successful invocation marks its publication
`COMPLETED`; an interrupted or failed delivery remains visible for controlled
resubmission.

The application deliberately claims **at-least-once**, not exactly-once,
delivery. Events carry stable identifiers, and both implemented consumers are
idempotent. Automatic replay at startup stays disabled so operators do not
trigger an unbounded recovery storm; a later operations milestone will expose
metrics and an explicit resubmission procedure before production deployment.

The query module will be explicitly eventually consistent. Its projection uses
a monotonic workflow rank so delayed events cannot move a return case backwards.
