package io.github.wasiliystrecker.returns.inspection.domain;

/** Signals invalid inspection state or input. */
public final class InvalidInspectionException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public InvalidInspectionException(String message) {
    super(message);
  }
}
