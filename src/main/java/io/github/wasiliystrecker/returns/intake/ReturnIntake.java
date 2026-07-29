package io.github.wasiliystrecker.returns.intake;

/** Public facade of the return intake module. */
public interface ReturnIntake {

  /**
   * Validates and persists one return request.
   *
   * @param command request data
   * @return the stable return identifier and acceptance time
   * @throws DuplicateReturnRequestException if the order item was already submitted
   */
  ReturnReceipt request(RequestReturnCommand command);
}
