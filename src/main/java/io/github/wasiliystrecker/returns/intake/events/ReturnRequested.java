package io.github.wasiliystrecker.returns.intake.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable event emitted while the return intake transaction is completing.
 *
 * <p>The payload intentionally contains identifiers and scalar values rather than an internal
 * aggregate or persistence entity.
 */
public record ReturnRequested(
    UUID eventId,
    UUID returnId,
    String orderReference,
    String itemReference,
    String reason,
    long refundMinorUnits,
    String currency,
    Instant occurredAt) {

  public ReturnRequested {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(orderReference, "orderReference");
    Objects.requireNonNull(itemReference, "itemReference");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(occurredAt, "occurredAt");
    if (refundMinorUnits <= 0) {
      throw new IllegalArgumentException("refundMinorUnits must be positive");
    }
  }
}
