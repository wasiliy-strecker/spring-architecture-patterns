package io.github.wasiliystrecker.returns.intake.adapter.web;

import io.github.wasiliystrecker.returns.ApiProblemDetails;
import io.github.wasiliystrecker.returns.intake.DuplicateReturnRequestException;
import io.github.wasiliystrecker.returns.intake.domain.InvalidReturnRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ReturnIntakeController.class)
final class IntakeApiExceptionHandler {

  @ExceptionHandler(InvalidReturnRequestException.class)
  ProblemDetail handleInvalidReturn(
      InvalidReturnRequestException exception, HttpServletRequest request) {
    return ApiProblemDetails.create(
        HttpStatus.BAD_REQUEST,
        "invalid-return-request",
        "INVALID_RETURN_REQUEST",
        "Invalid return request",
        exception.getMessage(),
        request);
  }

  @ExceptionHandler(DuplicateReturnRequestException.class)
  ProblemDetail handleDuplicate(
      DuplicateReturnRequestException exception, HttpServletRequest request) {
    return ApiProblemDetails.create(
        HttpStatus.CONFLICT,
        "return-already-requested",
        "RETURN_ALREADY_REQUESTED",
        "Return already requested",
        exception.getMessage(),
        request);
  }
}
