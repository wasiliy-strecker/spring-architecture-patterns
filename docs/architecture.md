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
    O[operations] -->|diagnostics| EP[(event_publication)]
```

The complete path from `intake` through `refund` is implemented. `query`
subscribes independently to every public event and builds the read side without
introducing synchronous dependencies back into the write modules.
`operations` owns the operator-facing view and recovery port for the durable
event registry without reaching into any business module.

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

## Refund and query slices

```mermaid
flowchart LR
    RA[ReturnApproved] --> SL[ReturnApprovedListener]
    SL --> SR[ScheduleRefundService]
    SR --> FP[(refund_payment)]
    SR --> RS[RefundScheduled]
    API[RefundOperations] --> SS[SettleRefundService]
    SS --> FP
    SS --> RC[RefundCompleted]
    E[All public workflow events] --> PL[ReturnCaseEventListeners]
    PL --> PS[ProjectReturnCaseService]
    PS --> EV[(return_case_projection_event)]
    PS --> VIEW[(return_case_view)]
    Q[ReturnCaseQueries] --> VIEW
```

The refund aggregate distinguishes instruction creation from provider
settlement. Scheduling is atomic on return and source-event identifiers.
Settlement stores an immutable provider reference, treats the same
acknowledgement as an idempotent retry, and uses optimistic locking to reject
competing rewrites. No database transaction is presented as a guarantee over an
external payment network.

The query module is a CQRS read side. A processed-event ledger deduplicates
stable event IDs in the same transaction as the projection update. Sparse
upserts allow later workflow events to arrive before intake data. A workflow
rank can advance but never decrease, while missing lower-rank attributes are
still filled when delayed events arrive.

## HTTP security and operations boundaries

```mermaid
flowchart LR
    C[OAuth client] -->|JWT + least-privilege scope| W[Module web adapters]
    W --> I[intake API]
    W --> N[inspection API]
    W --> F[refund API]
    W --> Q[query API]
    P[Platform probe] --> H[liveness / readiness]
    A[Operator] -->|operations:read| M[Prometheus + backlog]
    A -->|operations:manage| X[bounded resubmission]
    X --> E[(event_publication)]
```

The HTTP layer is an adapter, not a second application model. Request records
perform transport-level shape validation and then translate to the public
module commands. Domain invariants remain authoritative. Responses use
transport records where creation or command acknowledgement semantics differ
from the internal use-case result; the query API intentionally exposes its
read-optimized view.

Spring Security runs as a stateless OAuth 2.0 resource server. RSA signatures,
issuer, audience, time bounds, and operation-specific scopes are checked before
a controller is invoked. Only probes and RFC 9728 protected-resource metadata
are public. RFC 6750 authentication headers are preserved, while RFC 9457
problem details add a stable machine code and a safe request identifier shared
with logs and the response header.

The `operations` module follows the same adapter-to-application direction as
the business modules. Its JDBC adapter observes registry state, its Modulith
adapter performs resubmission, and the application service coordinates a
bounded command using a clock and framework-independent ports. Prometheus
gauges read one periodically refreshed snapshot instead of querying PostgreSQL
once per gauge during every scrape.

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
`RefundModuleIT` verifies scheduling and provider acknowledgement, while
`QueryModuleIT` deliberately publishes events in reverse order.
`ReturnWorkflowIT` verifies the complete asynchronous path, publication
completion, optimistic version increments, duplicate delivery, settlement, and
the final read view against real PostgreSQL.
`SecuredRestApiTest` exercises the complete MVC contract without infrastructure:
scope isolation, OAuth metadata, validation, business problems, and correlation.
`OperationsEndpointIT` verifies public probes, protected diagnostics, bounded
recovery, and Prometheus output against the real event registry.

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
delivery. Events carry stable identifiers, write-side consumers use atomic
uniqueness constraints, and the query side keeps a processed-event ledger.
Automatic replay at startup stays disabled so operators do not trigger an
unbounded recovery storm. Operators inspect
`GET /actuator/eventpublications`, alert on the Prometheus backlog metrics, and
use `POST /actuator/eventpublications` with a minimum age, in-flight limit, and
batch size. The response reports an observed eligible count rather than
claiming synchronous delivery success, and each accepted request is audit
logged with the authenticated subject.

The query API is explicitly eventually consistent. Callers must therefore
tolerate a short period in which a newly submitted return has no view yet.
