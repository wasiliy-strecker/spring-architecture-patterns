package io.github.wasiliystrecker.returns.inspection;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.PostgresIntegrationTest;
import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

@ApplicationModuleTest
final class InspectionModuleIT extends PostgresIntegrationTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final Instant REQUESTED_AT = Instant.parse("2026-07-29T08:00:00Z");

  @Autowired private InspectionWork inspectionWork;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearModuleState() {
    jdbcTemplate.update(
        "TRUNCATE TABLE event_publication, return_resolution, inspection_case, return_request");
  }

  @Test
  void consumesIntakeContractAndPublishesCompletionContract(Scenario scenario) {
    ReturnRequested requested =
        new ReturnRequested(
            UUID.fromString("a2fe74ac-5fde-4418-8154-e06fdcaed1ea"),
            RETURN_ID,
            "ORDER-1001",
            "LINE-2",
            "DAMAGED",
            12_500,
            "EUR",
            REQUESTED_AT);

    scenario
        .publish(requested)
        .andWaitAtMost(Duration.ofSeconds(10))
        .andWaitForStateChange(this::inspectionCount, count -> count == 1)
        .andVerify(count -> assertThat(count).isEqualTo(1));

    scenario
        .stimulate(
            () ->
                inspectionWork.complete(
                    new CompleteInspectionCommand(
                        RETURN_ID, "ACCEPTED", "Item matches the request.")))
        .andWaitAtMost(Duration.ofSeconds(10))
        .andWaitForEventOfType(InspectionCompleted.class)
        .matching(event -> event.returnId().equals(RETURN_ID))
        .toArriveAndVerify(
            (event, receipt) -> {
              assertThat(event.outcome()).isEqualTo("ACCEPTED");
              assertThat(event.refundMinorUnits()).isEqualTo(12_500);
              assertThat(receipt.returnId()).isEqualTo(RETURN_ID);
            });
  }

  private int inspectionCount() {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM inspection_case WHERE return_id = ?", Integer.class, RETURN_ID);
  }
}
