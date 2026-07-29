package io.github.wasiliystrecker.returns.resolution.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Signals that a return was rejected and exposes a stable machine-readable reason. */
public record ReturnRejected(UUID eventId, UUID returnId, String reason, Instant decidedAt)
    implements ResolutionEvent {

  public ReturnRejected {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(reason, "reason");
    Objects.requireNonNull(decidedAt, "decidedAt");
  }
}
