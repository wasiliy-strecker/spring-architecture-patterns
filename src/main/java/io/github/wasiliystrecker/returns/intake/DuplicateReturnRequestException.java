package io.github.wasiliystrecker.returns.intake;

/** Signals that the same order item has already entered the returns workflow. */
public final class DuplicateReturnRequestException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public DuplicateReturnRequestException(String orderReference, String itemReference) {
    super(message(orderReference, itemReference));
  }

  public DuplicateReturnRequestException(
      String orderReference, String itemReference, Throwable cause) {
    super(message(orderReference, itemReference), cause);
  }

  private static String message(String orderReference, String itemReference) {
    return "Return already requested for order %s and item %s"
        .formatted(orderReference, itemReference);
  }
}
