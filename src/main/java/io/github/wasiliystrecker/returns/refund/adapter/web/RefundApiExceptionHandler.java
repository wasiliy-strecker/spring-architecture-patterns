package io.github.wasiliystrecker.returns.refund.adapter.web;

import io.github.wasiliystrecker.returns.ApiProblemDetails;
import io.github.wasiliystrecker.returns.refund.RefundAlreadyCompletedException;
import io.github.wasiliystrecker.returns.refund.RefundNotFoundException;
import io.github.wasiliystrecker.returns.refund.domain.InvalidRefundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = RefundController.class)
final class RefundApiExceptionHandler {

  @ExceptionHandler(RefundNotFoundException.class)
  ProblemDetail handleNotFound(RefundNotFoundException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.NOT_FOUND,
        "refund-not-found",
        "REFUND_NOT_FOUND",
        "Refund not found",
        exception,
        request);
  }

  @ExceptionHandler(RefundAlreadyCompletedException.class)
  ProblemDetail handleCompleted(
      RefundAlreadyCompletedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "refund-already-completed",
        "REFUND_ALREADY_COMPLETED",
        "Refund already completed",
        exception,
        request);
  }

  @ExceptionHandler(InvalidRefundException.class)
  ProblemDetail handleInvalid(InvalidRefundException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "invalid-refund",
        "INVALID_REFUND",
        "Invalid refund",
        exception,
        request);
  }

  private static ProblemDetail problem(
      HttpStatus status,
      String type,
      String code,
      String title,
      RuntimeException exception,
      HttpServletRequest request) {
    return ApiProblemDetails.create(status, type, code, title, exception.getMessage(), request);
  }
}
