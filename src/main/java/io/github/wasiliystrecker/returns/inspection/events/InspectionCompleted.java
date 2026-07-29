package io.github.wasiliystrecker.returns.inspection.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Stable cross-module contract emitted after the physical inspection is recorded. */
public record InspectionCompleted(
    UUID eventId,
    UUID returnId,
    String outcome,
    long refundMinorUnits,
    String currency,
    Instant completedAt) {

  public InspectionCompleted {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(outcome, "outcome");
    Objects.requireNonNull(currency, "currency");
    Objects.requireNonNull(completedAt, "completedAt");
    if (refundMinorUnits <= 0) {
      throw new IllegalArgumentException("refundMinorUnits must be positive");
    }
  }
}
