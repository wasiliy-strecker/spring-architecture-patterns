package io.github.wasiliystrecker.returns.intake.domain;

/** Signals that a return request violates a domain invariant. */
public final class InvalidReturnRequestException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public InvalidReturnRequestException(String message) {
    super(message);
  }
}
