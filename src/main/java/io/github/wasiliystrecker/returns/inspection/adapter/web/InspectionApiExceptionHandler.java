package io.github.wasiliystrecker.returns.inspection.adapter.web;

import io.github.wasiliystrecker.returns.ApiProblemDetails;
import io.github.wasiliystrecker.returns.inspection.InspectionAlreadyCompletedException;
import io.github.wasiliystrecker.returns.inspection.InspectionNotFoundException;
import io.github.wasiliystrecker.returns.inspection.domain.InvalidInspectionException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = InspectionController.class)
final class InspectionApiExceptionHandler {

  @ExceptionHandler(InspectionNotFoundException.class)
  ProblemDetail handleNotFound(InspectionNotFoundException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.NOT_FOUND,
        "inspection-not-found",
        "INSPECTION_NOT_FOUND",
        "Inspection not found",
        exception,
        request);
  }

  @ExceptionHandler(InspectionAlreadyCompletedException.class)
  ProblemDetail handleCompleted(
      InspectionAlreadyCompletedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        "inspection-already-completed",
        "INSPECTION_ALREADY_COMPLETED",
        "Inspection already completed",
        exception,
        request);
  }

  @ExceptionHandler(InvalidInspectionException.class)
  ProblemDetail handleInvalid(InvalidInspectionException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "invalid-inspection",
        "INVALID_INSPECTION",
        "Invalid inspection",
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
