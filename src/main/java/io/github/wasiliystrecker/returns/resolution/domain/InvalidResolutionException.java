package io.github.wasiliystrecker.returns.resolution.domain;

/** Signals invalid resolution input or persisted state. */
public final class InvalidResolutionException extends IllegalArgumentException {
  private static final long serialVersionUID = 1L;

  public InvalidResolutionException(String message) {
    super(message);
  }
}
