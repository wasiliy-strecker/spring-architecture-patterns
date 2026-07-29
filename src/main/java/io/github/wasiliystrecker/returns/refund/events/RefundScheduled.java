package io.github.wasiliystrecker.returns.refund.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals that an approved return has one durable refund instruction. */
public record RefundScheduled(
    UUID eventId,
    UUID refundId,
    UUID returnId,
    long refundMinorUnits,
    String currency,
    Instant occurredAt)
    implements RefundEvent {

  public RefundScheduled {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(refundId, "refundId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(occurredAt, "occurredAt");
    if (refundMinorUnits <= 0) {
      throw new IllegalArgumentException("refundMinorUnits must be positive");
    }
  }
}
