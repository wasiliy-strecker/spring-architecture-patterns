package io.github.wasiliystrecker.returns.intake.application;

import io.github.wasiliystrecker.returns.intake.DuplicateReturnRequestException;
import io.github.wasiliystrecker.returns.intake.RequestReturnCommand;
import io.github.wasiliystrecker.returns.intake.ReturnIntake;
import io.github.wasiliystrecker.returns.intake.ReturnReceipt;
import io.github.wasiliystrecker.returns.intake.domain.Money;
import io.github.wasiliystrecker.returns.intake.domain.ReturnReason;
import io.github.wasiliystrecker.returns.intake.domain.ReturnRequest;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Framework-independent implementation of the return intake use case. */
public final class RequestReturnService implements ReturnIntake {
  private final ReturnRequestRepository requests;
  private final DomainEventPublisher events;
  private final TransactionRunner transactions;
  private final IdentifierGenerator identifiers;
  private final Clock clock;

  public RequestReturnService(
      ReturnRequestRepository requests,
      DomainEventPublisher events,
      TransactionRunner transactions,
      IdentifierGenerator identifiers,
      Clock clock) {
    this.requests = Objects.requireNonNull(requests, "requests");
    this.events = Objects.requireNonNull(events, "events");
    this.transactions = Objects.requireNonNull(transactions, "transactions");
    this.identifiers = Objects.requireNonNull(identifiers, "identifiers");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public ReturnReceipt request(RequestReturnCommand command) {
    Objects.requireNonNull(command, "command");
    return transactions.required(() -> requestWithinTransaction(command));
  }

  private ReturnReceipt requestWithinTransaction(RequestReturnCommand command) {
    Instant requestedAt = clock.instant();
    ReturnRequest request =
        ReturnRequest.request(
            identifiers.next(),
            command.orderReference(),
            command.itemReference(),
            ReturnReason.from(command.reason()),
            command.comment(),
            new Money(command.requestedRefundMinorUnits(), command.currency()),
            requestedAt);

    if (requests.exists(request.orderReference(), request.itemReference())) {
      throw new DuplicateReturnRequestException(request.orderReference(), request.itemReference());
    }

    requests.add(request);
    events.publish(
        new ReturnRequested(
            identifiers.next(),
            request.id(),
            request.orderReference(),
            request.itemReference(),
            request.reason().name(),
            request.requestedRefund().minorUnits(),
            request.requestedRefund().currency(),
            requestedAt));
    return new ReturnReceipt(request.id(), requestedAt);
  }
}
