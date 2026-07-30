package io.github.wasiliystrecker.returns;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Propagates a safe request identifier into responses, logs, and API errors. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class RequestCorrelationFilter extends OncePerRequestFilter {

  public static final String HEADER_NAME = "X-Request-Id";
  public static final String ATTRIBUTE_NAME =
      RequestCorrelationFilter.class.getName() + ".requestId";

  private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestId = resolveRequestId(request.getHeader(HEADER_NAME));

    request.setAttribute(ATTRIBUTE_NAME, requestId);
    response.setHeader(HEADER_NAME, requestId);

    String previousRequestId = MDC.get("requestId");
    MDC.put("requestId", requestId);
    try {
      filterChain.doFilter(request, response);
    } finally {
      if (previousRequestId == null) {
        MDC.remove("requestId");
      } else {
        MDC.put("requestId", previousRequestId);
      }
    }
  }

  private static String resolveRequestId(String candidate) {
    return candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()
        ? candidate
        : UUID.randomUUID().toString();
  }
}
