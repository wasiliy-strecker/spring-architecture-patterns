package io.github.wasiliystrecker.returns;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiValidationExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleInvalidBody(
      MethodArgumentNotValidException exception, HttpServletRequest request) {
    List<Violation> violations =
        exception.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(error -> new Violation(error.getField(), error.getDefaultMessage()))
            .toList();

    ProblemDetail problem = invalidRequest("The request body contains invalid values.", request);
    problem.setProperty("violations", violations);
    return problem;
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class
  })
  ProblemDetail handleInvalidParameter(Exception exception, HttpServletRequest request) {
    return invalidRequest("A request parameter or path value is invalid.", request);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail handleUnreadableBody(
      HttpMessageNotReadableException exception, HttpServletRequest request) {
    return invalidRequest("The request body is missing or malformed.", request);
  }

  private static ProblemDetail invalidRequest(String detail, HttpServletRequest request) {
    return ApiProblemDetails.create(
        HttpStatus.BAD_REQUEST,
        "invalid-request",
        "INVALID_REQUEST",
        "Invalid request",
        detail,
        request);
  }

  private record Violation(String field, String message) {}
}
