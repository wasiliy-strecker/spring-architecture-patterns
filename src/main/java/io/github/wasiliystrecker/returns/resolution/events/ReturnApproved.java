package io.github.wasiliystrecker.returns.resolution.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals that a return is eligible for a full refund. */
public record ReturnApproved(
    UUID eventId, UUID returnId, long refundMinorUnits, String currency, Instant decidedAt)
    implements ResolutionEvent {

  public ReturnApproved {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(decidedAt, "decidedAt");
    if (refundMinorUnits <= 0) {
      throw new IllegalArgumentException("refundMinorUnits must be positive");
    }
  }
}
