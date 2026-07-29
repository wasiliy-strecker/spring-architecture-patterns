package io.github.wasiliystrecker.returns.inspection;

import java.util.UUID;

/** Raised when a completed inspection is submitted a second time. */
public final class InspectionAlreadyCompletedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InspectionAlreadyCompletedException(UUID returnId) {
    super("Inspection for return %s is already completed".formatted(returnId));
  }

  public InspectionAlreadyCompletedException(UUID returnId, Throwable cause) {
    super("Inspection for return %s was completed concurrently".formatted(returnId), cause);
  }
}
