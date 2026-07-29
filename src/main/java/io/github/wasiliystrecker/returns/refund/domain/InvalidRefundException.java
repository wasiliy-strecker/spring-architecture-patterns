package io.github.wasiliystrecker.returns.refund.domain;

/** Signals invalid refund input or persisted state. */
public final class InvalidRefundException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public InvalidRefundException(String message) {
    super(message);
  }
}
