package io.github.wasiliystrecker.returns.refund.application;

import io.github.wasiliystrecker.returns.refund.RefundNotFoundException;
import io.github.wasiliystrecker.returns.refund.RefundOperations;
import io.github.wasiliystrecker.returns.refund.RefundReceipt;
import io.github.wasiliystrecker.returns.refund.SettleRefundCommand;
import io.github.wasiliystrecker.returns.refund.domain.RefundPayment;
import io.github.wasiliystrecker.returns.refund.domain.RefundStatus;
import io.github.wasiliystrecker.returns.refund.events.RefundCompleted;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/** Records provider settlement acknowledgements without coupling the core to a payment SDK. */
public final class SettleRefundService implements RefundOperations {
  private final RefundPaymentRepository refunds;
  private final RefundEventPublisher events;
  private final TransactionRunner transactions;
  private final IdentifierGenerator identifiers;
  private final Clock clock;

  public SettleRefundService(
      RefundPaymentRepository refunds,
      RefundEventPublisher events,
      TransactionRunner transactions,
      IdentifierGenerator identifiers,
      Clock clock) {
    this.refunds = Objects.requireNonNull(refunds, "refunds");
    this.events = Objects.requireNonNull(events, "events");
    this.transactions = Objects.requireNonNull(transactions, "transactions");
    this.identifiers = Objects.requireNonNull(identifiers, "identifiers");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public RefundReceipt settle(SettleRefundCommand command) {
    Objects.requireNonNull(command, "command");
    Objects.requireNonNull(command.returnId(), "returnId");
    return transactions.required(() -> settleWithinTransaction(command));
  }

  private RefundReceipt settleWithinTransaction(SettleRefundCommand command) {
    RefundPayment current =
        refunds
            .findByReturnId(command.returnId())
            .orElseThrow(() -> new RefundNotFoundException(command.returnId()));
    Instant settledAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
    RefundPayment settled = current.settle(command.providerReference(), settledAt);
    if (current.status() == RefundStatus.COMPLETED) {
      return receipt(current);
    }

    refunds.update(settled);
    events.publish(
        new RefundCompleted(
            identifiers.next(),
            settled.refundId(),
            settled.returnId(),
            settled.providerReference(),
            settledAt));
    return receipt(settled);
  }

  private static RefundReceipt receipt(RefundPayment refund) {
    return new RefundReceipt(
        refund.refundId(), refund.returnId(), refund.status().name(), refund.settledAt());
  }
}
