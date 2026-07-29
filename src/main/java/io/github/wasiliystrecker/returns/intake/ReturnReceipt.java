package io.github.wasiliystrecker.returns.intake;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Stable result returned after a return request is accepted. */
public record ReturnReceipt(UUID returnId, Instant requestedAt) {

  public ReturnReceipt {
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(requestedAt, "requestedAt");
  }
}
