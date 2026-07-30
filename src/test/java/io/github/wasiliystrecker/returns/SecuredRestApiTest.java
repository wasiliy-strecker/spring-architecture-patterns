package io.github.wasiliystrecker.returns;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.wasiliystrecker.returns.inspection.InspectionReceipt;
import io.github.wasiliystrecker.returns.inspection.InspectionWork;
import io.github.wasiliystrecker.returns.inspection.adapter.web.InspectionController;
import io.github.wasiliystrecker.returns.intake.DuplicateReturnRequestException;
import io.github.wasiliystrecker.returns.intake.ReturnIntake;
import io.github.wasiliystrecker.returns.intake.ReturnReceipt;
import io.github.wasiliystrecker.returns.intake.adapter.web.ReturnIntakeController;
import io.github.wasiliystrecker.returns.query.ReturnCaseQueries;
import io.github.wasiliystrecker.returns.query.ReturnCaseView;
import io.github.wasiliystrecker.returns.query.adapter.web.ReturnCaseController;
import io.github.wasiliystrecker.returns.refund.RefundOperations;
import io.github.wasiliystrecker.returns.refund.RefundReceipt;
import io.github.wasiliystrecker.returns.refund.adapter.web.RefundController;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {
      ReturnIntakeController.class,
      InspectionController.class,
      RefundController.class,
      ReturnCaseController.class
    })
@Import({
  ApiSecurityConfiguration.class,
  ApiValidationExceptionHandler.class,
  RequestCorrelationFilter.class,
  ServletWebSecurityAutoConfiguration.class,
  SecuredRestApiTest.FacadeStubs.class
})
final class SecuredRestApiTest {

  private static final UUID RETURN_ID = UUID.fromString("6dca7023-9098-4936-9e8c-1a48082ddc13");
  private static final UUID REFUND_ID = UUID.fromString("11cd1052-a359-4bd4-a3fe-3ea0be5d31a7");
  private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

  private final MockMvc mockMvc;

  @Autowired
  SecuredRestApiTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void requiresABearerTokenAndReturnsCorrelatedProblemDetails() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/returns/{returnId}", RETURN_ID)
                .header(RequestCorrelationFilter.HEADER_NAME, "request-401"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(header().string(RequestCorrelationFilter.HEADER_NAME, "request-401"))
        .andExpect(
            header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("Bearer")))
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
        .andExpect(jsonPath("$.requestId").value("request-401"));
  }

  @Test
  void publishesOAuthProtectedResourceMetadataWithoutAuthentication() throws Exception {
    mockMvc
        .perform(get("/.well-known/oauth-protected-resource"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resource_name").value("Returns workflow API"))
        .andExpect(
            jsonPath(
                "$.scopes_supported",
                org.hamcrest.Matchers.hasItems(
                    "returns:read", "returns:write", "operations:manage")));
  }

  @Test
  void rejectsAValidTokenWithoutTheRequiredScope() throws Exception {
    mockMvc
        .perform(get("/api/v1/returns/{returnId}", RETURN_ID).with(scope("returns:write")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("INSUFFICIENT_SCOPE"));
  }

  @Test
  void rejectsStateChangingRequestsWithoutCsrfProtectionOutsideBearerAuthentication()
      throws Exception {
    mockMvc
        .perform(
            post("/api/v1/returns")
                .with(
                    user("session-user")
                        .authorities(new SimpleGrantedAuthority("SCOPE_returns:write"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void createsAReturnThroughTheWriteScope() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/returns")
                .with(scope("returns:write"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "orderReference": "ORDER-5005",
                      "itemReference": "LINE-3",
                      "reason": "DAMAGED",
                      "comment": "Outer case cracked.",
                      "requestedRefundMinorUnits": 12900,
                      "currency": "EUR"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/returns/" + RETURN_ID))
        .andExpect(jsonPath("$.returnId").value(RETURN_ID.toString()))
        .andExpect(jsonPath("$.requestedAt").value(NOW.toString()));
  }

  @Test
  void rejectsInvalidJsonWithFieldLevelViolations() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/returns")
                .with(scope("returns:write"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "orderReference": "",
                      "itemReference": "L",
                      "reason": "",
                      "requestedRefundMinorUnits": 0,
                      "currency": "EURO"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.violations").isArray());
  }

  @Test
  void exposesTheRemainingWorkflowCommandsUnderDistinctScopes() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/returns/{returnId}/inspection", RETURN_ID)
                .with(scope("returns:inspect"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"outcome": "ACCEPTED", "note": "Damage confirmed."}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("ACCEPTED"));

    mockMvc
        .perform(
            put("/api/v1/returns/{returnId}/refund", RETURN_ID)
                .with(scope("refunds:settle"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"providerReference": "PSP-ORDER-5005"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refundId").value(REFUND_ID.toString()))
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void readsTheEventuallyConsistentView() throws Exception {
    mockMvc
        .perform(get("/api/v1/returns/{returnId}", RETURN_ID).with(scope("returns:read")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.returnId").value(RETURN_ID.toString()))
        .andExpect(jsonPath("$.status").value("REFUNDED"))
        .andExpect(jsonPath("$.projectionVersion").value(6));
  }

  @Test
  void mapsBusinessConflictsAndMissingViewsToStableProblems() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/returns")
                .with(scope("returns:write"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "orderReference": "ORDER-DUPLICATE",
                      "itemReference": "LINE-3",
                      "reason": "DAMAGED",
                      "requestedRefundMinorUnits": 12900,
                      "currency": "EUR"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("RETURN_ALREADY_REQUESTED"));

    mockMvc
        .perform(get("/api/v1/returns/{returnId}", UUID.randomUUID()).with(scope("returns:read")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RETURN_CASE_NOT_FOUND"));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor scope(
      String scope) {
    return jwt()
        .jwt(token -> token.subject("portfolio-reviewer"))
        .authorities(new SimpleGrantedAuthority("SCOPE_" + scope));
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FacadeStubs {

    @Bean
    ReturnIntake returnIntake() {
      return command -> {
        if ("ORDER-DUPLICATE".equals(command.orderReference())) {
          throw new DuplicateReturnRequestException(
              command.orderReference(), command.itemReference());
        }
        return new ReturnReceipt(RETURN_ID, NOW);
      };
    }

    @Bean
    InspectionWork inspectionWork() {
      return command -> new InspectionReceipt(command.returnId(), "ACCEPTED", NOW);
    }

    @Bean
    RefundOperations refundOperations() {
      return command -> new RefundReceipt(REFUND_ID, command.returnId(), "COMPLETED", NOW);
    }

    @Bean
    ReturnCaseQueries returnCaseQueries() {
      return returnId ->
          RETURN_ID.equals(returnId)
              ? Optional.of(
                  new ReturnCaseView(
                      returnId,
                      "ORDER-5005",
                      "LINE-3",
                      "DAMAGED",
                      "REFUNDED",
                      12_900,
                      "EUR",
                      "ACCEPTED",
                      null,
                      REFUND_ID,
                      "COMPLETED",
                      NOW,
                      6))
              : Optional.empty();
    }

    @Bean
    JwtDecoder jwtDecoder() {
      return token -> {
        throw new JwtException("Raw token decoding is outside this MVC slice");
      };
    }
  }
}
