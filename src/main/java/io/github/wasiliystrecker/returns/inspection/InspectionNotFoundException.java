package io.github.wasiliystrecker.returns.inspection;

import java.util.UUID;

/** Raised when inspection work has not yet been registered for a return. */
public final class InspectionNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InspectionNotFoundException(UUID returnId) {
    super("No inspection exists for return %s".formatted(returnId));
  }
}
