package io.github.wasiliystrecker.returns;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Shared HTTP error representation used only by inbound adapters. */
public final class ApiProblemDetails {

  private static final String TYPE_BASE = "https://wasiliy-strecker.github.io/problems/";

  private ApiProblemDetails() {}

  public static ProblemDetail create(
      HttpStatus status,
      String type,
      String code,
      String title,
      String detail,
      HttpServletRequest request) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(URI.create(TYPE_BASE + type));
    problem.setTitle(title);
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("code", code);
    problem.setProperty("requestId", request.getAttribute(RequestCorrelationFilter.ATTRIBUTE_NAME));
    return problem;
  }
}
