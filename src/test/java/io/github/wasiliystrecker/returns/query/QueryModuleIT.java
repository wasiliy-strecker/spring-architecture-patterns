package io.github.wasiliystrecker.returns.query;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.PostgresIntegrationTest;
import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import io.github.wasiliystrecker.returns.query.application.ProjectReturnCaseService;
import io.github.wasiliystrecker.returns.refund.events.RefundCompleted;
import io.github.wasiliystrecker.returns.refund.events.RefundScheduled;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import io.github.wasiliystrecker.returns.resolution.events.ReturnRejected;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

@ApplicationModuleTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
final class QueryModuleIT extends PostgresIntegrationTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID REFUND_ID = UUID.fromString("cd11bbf1-a2ae-4c01-ac71-bd7fd3018ee2");

  @Autowired private ReturnCaseQueries queries;
  @Autowired private ProjectReturnCaseService projector;
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
  void keepsFinalStateWhenEventsArriveInReverseOrder(Scenario scenario) {
    RefundCompleted completed =
        new RefundCompleted(
            UUID.fromString("5413aa62-65ff-41b3-8799-08a6eaf95355"),
            REFUND_ID,
            RETURN_ID,
            "PSP-84729",
            Instant.parse("2026-07-29T11:00:00Z"));
    RefundScheduled scheduled =
        new RefundScheduled(
            UUID.fromString("58a17d45-41de-43bc-9c64-690b513a1e36"),
            REFUND_ID,
            RETURN_ID,
            12_500,
            "EUR",
            Instant.parse("2026-07-29T10:00:00Z"));
    InspectionCompleted inspected =
        new InspectionCompleted(
            UUID.fromString("a249b0c0-ae77-4847-87b4-9af5428215bb"),
            RETURN_ID,
            "ACCEPTED",
            12_500,
            "EUR",
            Instant.parse("2026-07-29T09:00:00Z"));
    ReturnApproved approved =
        new ReturnApproved(
            UUID.fromString("c799f702-c615-4297-9e84-c9fe61808b41"),
            RETURN_ID,
            12_500,
            "EUR",
            Instant.parse("2026-07-29T09:01:00Z"));
    ReturnRequested requested =
        new ReturnRequested(
            UUID.fromString("a2fe74ac-5fde-4418-8154-e06fdcaed1ea"),
            RETURN_ID,
            "ORDER-1001",
            "LINE-2",
            "DAMAGED",
            12_500,
            "EUR",
            Instant.parse("2026-07-29T08:00:00Z"));

    publishAndWaitForVersion(scenario, completed, 1);
    publishAndWaitForVersion(scenario, scheduled, 2);
    publishAndWaitForVersion(scenario, inspected, 3);
    publishAndWaitForVersion(scenario, approved, 4);
    publishAndWaitForVersion(scenario, requested, 5);

    assertThat(queries.findById(RETURN_ID))
        .get()
        .satisfies(
            view -> {
              assertThat(view.orderReference()).isEqualTo("ORDER-1001");
              assertThat(view.status()).isEqualTo("REFUNDED");
              assertThat(view.inspectionOutcome()).isEqualTo("ACCEPTED");
              assertThat(view.refundId()).isEqualTo(REFUND_ID);
              assertThat(view.refundStatus()).isEqualTo("COMPLETED");
              assertThat(view.lastUpdatedAt()).isEqualTo(completed.occurredAt());
              assertThat(view.projectionVersion()).isEqualTo(5);
            });

    projector.project(requested);
    assertThat(queries.findById(RETURN_ID))
        .get()
        .extracting(ReturnCaseView::projectionVersion)
        .isEqualTo(5L);
  }

  @Test
  void exposesARejectedReturnWithoutCreatingRefundFields(Scenario scenario) {
    ReturnRequested requested =
        new ReturnRequested(
            UUID.fromString("1a3dcc77-1d34-4f80-ae2e-bb09b45ca270"),
            RETURN_ID,
            "ORDER-REJECTED",
            "LINE-7",
            "NOT_AS_DESCRIBED",
            4_500,
            "GBP",
            Instant.parse("2026-07-29T08:00:00Z"));
    ReturnRejected rejected =
        new ReturnRejected(
            UUID.fromString("e08fc664-5cb5-4aa1-a618-9ae49991844e"),
            RETURN_ID,
            "INSPECTION_FAILED",
            Instant.parse("2026-07-29T09:00:00Z"));

    publishAndWaitForVersion(scenario, requested, 1);
    publishAndWaitForVersion(scenario, rejected, 2);

    assertThat(queries.findById(RETURN_ID))
        .get()
        .satisfies(
            view -> {
              assertThat(view.status()).isEqualTo("REJECTED");
              assertThat(view.rejectionReason()).isEqualTo("INSPECTION_FAILED");
              assertThat(view.refundId()).isNull();
              assertThat(view.refundStatus()).isNull();
            });
  }

  private void publishAndWaitForVersion(Scenario scenario, Object event, int version) {
    scenario
        .publish(event)
        .andWaitAtMost(Duration.ofSeconds(10))
        .andWaitForStateChange(() -> projectionVersion(RETURN_ID), current -> current == version)
        .andVerify(current -> assertThat(current).isEqualTo(version));
  }

  private int projectionVersion(UUID returnId) {
    return jdbcTemplate.queryForObject(
        """
        SELECT COALESCE(max(projection_version), 0)
          FROM return_case_view
         WHERE return_id = ?
        """,
        Integer.class,
        returnId);
  }
}
