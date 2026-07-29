package io.github.wasiliystrecker.returns.refund.application;

import io.github.wasiliystrecker.returns.refund.domain.RefundPayment;
import io.github.wasiliystrecker.returns.refund.events.RefundScheduled;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Idempotently creates one durable refund instruction from an approval event. */
public final class ScheduleRefundService {
  private final RefundPaymentRepository refunds;
  private final RefundEventPublisher events;
  private final IdentifierGenerator identifiers;
  private final Clock clock;

  public ScheduleRefundService(
      RefundPaymentRepository refunds,
      RefundEventPublisher events,
      IdentifierGenerator identifiers,
      Clock clock) {
    this.refunds = Objects.requireNonNull(refunds, "refunds");
    this.events = Objects.requireNonNull(events, "events");
    this.identifiers = Objects.requireNonNull(identifiers, "identifiers");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  public void schedule(ReturnApproved event) {
    Objects.requireNonNull(event, "event");
    if (refunds.exists(event.returnId(), event.eventId())) {
      return;
    }

    UUID refundId = identifiers.next();
    Instant scheduledAt = clock.instant();
    RefundPayment refund =
        RefundPayment.schedule(
            refundId,
            event.returnId(),
            event.eventId(),
            event.refundMinorUnits(),
            event.currency(),
            scheduledAt);
    if (refunds.addIfAbsent(refund)) {
      events.publish(
          new RefundScheduled(
              identifiers.next(),
              refundId,
              event.returnId(),
              event.refundMinorUnits(),
              event.currency(),
              scheduledAt));
    }
  }
}
