package io.github.wasiliystrecker.returns.refund;

import java.util.UUID;

/** Raised when a completed refund is changed to a different provider reference. */
public final class RefundAlreadyCompletedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RefundAlreadyCompletedException(UUID returnId) {
    super("Refund for return %s is already completed".formatted(returnId));
  }

  public RefundAlreadyCompletedException(UUID returnId, Throwable cause) {
    super("Refund for return %s was completed concurrently".formatted(returnId), cause);
  }
}
