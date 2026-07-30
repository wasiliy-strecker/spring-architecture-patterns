package io.github.wasiliystrecker.returns;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "returns.operations.metrics-initial-delay=PT0.1S",
      "returns.operations.metrics-refresh-interval=PT0.1S"
    })
@AutoConfigureMockMvc
final class OperationsEndpointIT extends PostgresIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearPublications() {
    jdbcTemplate.update("TRUNCATE TABLE event_publication");
  }

  @Test
  void exposesHealthButProtectsDiagnosticsAndBoundedRecovery() throws Exception {
    insertRecentIncompletePublication();

    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
    mockMvc.perform(get("/actuator/eventpublications")).andExpect(status().isUnauthorized());

    mockMvc
        .perform(get("/actuator/eventpublications").with(scope("operations:read")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.incomplete").value(1))
        .andExpect(jsonPath("$.failed").value(0))
        .andExpect(jsonPath("$.oldestPublicationAt").exists());

    mockMvc
        .perform(
            post("/actuator/eventpublications")
                .with(scope("operations:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "minAge": "PT5M",
                      "maxInFlight": 10,
                      "batchSize": 50
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.eligiblePublicationsObserved").value(0))
        .andExpect(jsonPath("$.minAge").value("PT5M"))
        .andExpect(jsonPath("$.maxInFlight").value(10));

    await()
        .atMost(Duration.ofSeconds(3))
        .untilAsserted(
            () ->
                mockMvc
                    .perform(get("/actuator/prometheus").with(scope("operations:read")))
                    .andExpect(status().isOk())
                    .andExpect(
                        content()
                            .string(
                                org.hamcrest.Matchers.containsString(
                                    "returns_event_publications_incomplete"
                                        + "{application=\"spring-architecture-patterns\"} 1.0"))));
  }

  private void insertRecentIncompletePublication() {
    jdbcTemplate.update(
        """
        INSERT INTO event_publication (
            id, listener_id, event_type, serialized_event, publication_date,
            completion_date, status, completion_attempts, last_resubmission_date
        )
        VALUES (?, ?, ?, ?, ?, NULL, 'PUBLISHED', 0, NULL)
        """,
        UUID.randomUUID(),
        "portfolio.operations",
        "example.Event",
        "{}",
        java.sql.Timestamp.from(Instant.now().minusSeconds(10)));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor scope(
      String scope) {
    return jwt()
        .jwt(token -> token.subject("operations-reviewer"))
        .authorities(new SimpleGrantedAuthority("SCOPE_" + scope));
  }
}
