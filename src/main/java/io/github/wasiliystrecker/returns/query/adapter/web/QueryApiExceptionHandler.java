package io.github.wasiliystrecker.returns.query.adapter.web;

import io.github.wasiliystrecker.returns.ApiProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ReturnCaseController.class)
final class QueryApiExceptionHandler {

  @ExceptionHandler(ReturnCaseNotFoundException.class)
  ProblemDetail handleNotFound(ReturnCaseNotFoundException exception, HttpServletRequest request) {
    return ApiProblemDetails.create(
        HttpStatus.NOT_FOUND,
        "return-case-not-found",
        "RETURN_CASE_NOT_FOUND",
        "Return case not found",
        exception.getMessage(),
        request);
  }
}
