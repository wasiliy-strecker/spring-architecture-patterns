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

The arrows describe the target design. The `intake` module now owns the first
complete business slice. The remaining modules are deliberately independent
until their behavior is implemented.

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

The generated documents live below `target/spring-modulith-docs` and are not
versioned; the source of truth remains the code and its verification tests.

## Delivery semantics

`ReturnRequested` is currently published synchronously inside the intake
transaction. A listener failure therefore rolls the insert back. This is tested
but is not yet a durable delivery guarantee.

The next event-driven milestone will add Spring Modulith's JDBC event publication
registry. Listeners will then execute asynchronously in their own transactions.
That provides at-least-once processing, not an exactly-once claim. Consumers
will therefore use stable event identifiers and idempotent state transitions.

The query module will be explicitly eventually consistent. Its projection uses
a monotonic workflow rank so delayed events cannot move a return case backwards.
