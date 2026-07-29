package io.github.wasiliystrecker.returns.inspection.application;

import io.github.wasiliystrecker.returns.inspection.CompleteInspectionCommand;
import io.github.wasiliystrecker.returns.inspection.InspectionNotFoundException;
import io.github.wasiliystrecker.returns.inspection.InspectionReceipt;
import io.github.wasiliystrecker.returns.inspection.InspectionWork;
import io.github.wasiliystrecker.returns.inspection.domain.InspectionCase;
import io.github.wasiliystrecker.returns.inspection.domain.InspectionOutcome;
import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Framework-independent orchestration of the inspection completion use case. */
public final class CompleteInspectionService implements InspectionWork {
  private final InspectionCaseRepository inspections;
  private final DomainEventPublisher events;
  private final TransactionRunner transactions;
  private final IdentifierGenerator identifiers;
  private final Clock clock;

  public CompleteInspectionService(
      InspectionCaseRepository inspections,
      DomainEventPublisher events,
      TransactionRunner transactions,
      IdentifierGenerator identifiers,
      Clock clock) {
    this.inspections = Objects.requireNonNull(inspections, "inspections");
    this.events = Objects.requireNonNull(events, "events");
    this.transactions = Objects.requireNonNull(transactions, "transactions");
    this.identifiers = Objects.requireNonNull(identifiers, "identifiers");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public InspectionReceipt complete(CompleteInspectionCommand command) {
    Objects.requireNonNull(command, "command");
    Objects.requireNonNull(command.returnId(), "returnId");
    return transactions.required(() -> completeWithinTransaction(command));
  }

  private InspectionReceipt completeWithinTransaction(CompleteInspectionCommand command) {
    InspectionCase current =
        inspections
            .findByReturnId(command.returnId())
            .orElseThrow(() -> new InspectionNotFoundException(command.returnId()));
    Instant completedAt = clock.instant();
    InspectionCase completed =
        current.complete(InspectionOutcome.from(command.outcome()), command.note(), completedAt);
    inspections.update(completed);
    events.publish(
        new InspectionCompleted(
            identifiers.next(),
            completed.returnId(),
            completed.outcome().name(),
            completed.refundMinorUnits(),
            completed.currency(),
            completedAt));
    return new InspectionReceipt(completed.returnId(), completed.outcome().name(), completedAt);
  }
}
