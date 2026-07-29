package io.github.wasiliystrecker.returns.refund.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals that the external payment provider acknowledged the settlement. */
public record RefundCompleted(
    UUID eventId, UUID refundId, UUID returnId, String providerReference, Instant occurredAt)
    implements RefundEvent {

  public RefundCompleted {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(refundId, "refundId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(providerReference, "providerReference");
    Objects.requireNonNull(occurredAt, "occurredAt");
  }
}
