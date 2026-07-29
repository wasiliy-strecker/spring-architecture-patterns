package io.github.wasiliystrecker.returns.refund;

import java.util.UUID;

/** Raised when an approved return has no refund instruction yet. */
public final class RefundNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public RefundNotFoundException(UUID returnId) {
    super("No refund exists for return %s".formatted(returnId));
  }
}
