package io.github.wasiliystrecker.returns.refund;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.wasiliystrecker.returns.PostgresIntegrationTest;
import io.github.wasiliystrecker.returns.refund.events.RefundCompleted;
import io.github.wasiliystrecker.returns.refund.events.RefundScheduled;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

@ApplicationModuleTest
final class RefundModuleIT extends PostgresIntegrationTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");

  @Autowired private RefundOperations refundOperations;
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
  void schedulesAndSettlesOneApprovedRefund(Scenario scenario) {
    scenario
        .publish(
            new ReturnApproved(
                UUID.fromString("c799f702-c615-4297-9e84-c9fe61808b41"),
                RETURN_ID,
                12_500,
                "EUR",
                Instant.parse("2026-07-29T09:00:00Z")))
        .andWaitAtMost(Duration.ofSeconds(10))
        .andWaitForEventOfType(RefundScheduled.class)
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
                assertThat(refundRow())
                    .containsEntry("status", "SCHEDULED")
                    .containsEntry("version", 0L));

    scenario
        .stimulate(() -> refundOperations.settle(new SettleRefundCommand(RETURN_ID, "PSP-84729")))
        .andWaitAtMost(Duration.ofSeconds(10))
        .andWaitForEventOfType(RefundCompleted.class)
        .matching(event -> event.returnId().equals(RETURN_ID))
        .toArriveAndVerify(
            (event, receipt) -> {
              assertThat(event.providerReference()).isEqualTo("PSP-84729");
              assertThat(receipt.status()).isEqualTo("COMPLETED");
            });

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(refundRow())
                    .containsEntry("status", "COMPLETED")
                    .containsEntry("provider_reference", "PSP-84729")
                    .containsEntry("version", 1L));
  }

  private Map<String, Object> refundRow() {
    return jdbcTemplate.queryForMap(
        """
        SELECT status, provider_reference, refund_minor_units, version
          FROM refund_payment
         WHERE return_id = ?
        """,
        RETURN_ID);
  }
}
