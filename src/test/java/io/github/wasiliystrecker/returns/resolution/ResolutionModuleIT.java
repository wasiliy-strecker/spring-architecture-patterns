package io.github.wasiliystrecker.returns.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.wasiliystrecker.returns.PostgresIntegrationTest;
import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import io.github.wasiliystrecker.returns.resolution.events.ReturnRejected;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

@ApplicationModuleTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
final class ResolutionModuleIT extends PostgresIntegrationTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final Instant COMPLETED_AT = Instant.parse("2026-07-29T09:00:00Z");

  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearModuleState() {
    jdbcTemplate.update(
        """
        TRUNCATE TABLE event_publication, return_case_projection_event,
            return_case_view, refund_payment, return_resolution,
            inspection_case, return_request
        """);
  }

  @Test
  void approvesAnAcceptedInspection(Scenario scenario) {
    scenario
        .publish(inspectionCompleted("ACCEPTED"))
        .andWaitAtMost(Duration.ofSeconds(10))
        .andWaitForEventOfType(ReturnApproved.class)
        .matching(event -> event.returnId().equals(RETURN_ID))
        .toArriveAndVerify(
            event -> {
              assertThat(event.refundMinorUnits()).isEqualTo(12_500);
              assertThat(event.currency()).isEqualTo("EUR");
            });

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(resolutionRow())
                    .containsEntry("status", "APPROVED")
                    .containsEntry("refund_minor_units", 12_500L)
                    .containsEntry("rejection_reason", null));
  }

  @Test
  void rejectsAFailedInspectionWithAStableReason(Scenario scenario) {
    scenario
        .publish(inspectionCompleted("REJECTED"))
        .andWaitAtMost(Duration.ofSeconds(10))
        .andWaitForEventOfType(ReturnRejected.class)
        .matching(event -> event.returnId().equals(RETURN_ID))
        .toArriveAndVerify(event -> assertThat(event.reason()).isEqualTo("INSPECTION_FAILED"));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(resolutionRow())
                    .containsEntry("status", "REJECTED")
                    .containsEntry("rejection_reason", "INSPECTION_FAILED"));
  }

  private InspectionCompleted inspectionCompleted(String outcome) {
    return new InspectionCompleted(
        UUID.fromString("a249b0c0-ae77-4847-87b4-9af5428215bb"),
        RETURN_ID,
        outcome,
        12_500,
        "EUR",
        COMPLETED_AT);
  }

  private Map<String, Object> resolutionRow() {
    return jdbcTemplate.queryForMap(
        """
        SELECT status, refund_minor_units, rejection_reason
          FROM return_resolution
         WHERE return_id = ?
        """,
        RETURN_ID);
  }
}
