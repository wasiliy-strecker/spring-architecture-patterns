package io.github.wasiliystrecker.returns;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import io.github.wasiliystrecker.returns.inspection.CompleteInspectionCommand;
import io.github.wasiliystrecker.returns.inspection.InspectionAlreadyCompletedException;
import io.github.wasiliystrecker.returns.inspection.InspectionWork;
import io.github.wasiliystrecker.returns.intake.RequestReturnCommand;
import io.github.wasiliystrecker.returns.intake.ReturnIntake;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import io.github.wasiliystrecker.returns.query.ReturnCaseQueries;
import io.github.wasiliystrecker.returns.query.ReturnCaseView;
import io.github.wasiliystrecker.returns.refund.RefundOperations;
import io.github.wasiliystrecker.returns.refund.SettleRefundCommand;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
final class ReturnWorkflowIT extends PostgresIntegrationTest {
  @Autowired private ReturnIntake returnIntake;
  @Autowired private InspectionWork inspectionWork;
  @Autowired private RefundOperations refundOperations;
  @Autowired private ReturnCaseQueries returnCaseQueries;
  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void clearWorkflowState() {
    jdbcTemplate.update(
        """
        TRUNCATE TABLE event_publication, return_case_projection_event,
            return_case_view, refund_payment, return_resolution,
            inspection_case, return_request
        """);
  }

  @Test
  void runsTheAcceptedPathAcrossDurablyConnectedModules() {
    var receipt =
        returnIntake.request(
            new RequestReturnCommand(
                "ORDER-3003",
                "LINE-4",
                "DAMAGED",
                "Housing cracked during transport.",
                18_900,
                "EUR"));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(inspectionRow(receipt.returnId()))
                    .containsEntry("status", "PENDING")
                    .containsEntry("refund_minor_units", 18_900L));

    inspectionWork.complete(
        new CompleteInspectionCommand(
            receipt.returnId(), "ACCEPTED", "Damage confirmed by warehouse."));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(resolutionRow(receipt.returnId()))
                    .containsEntry("status", "APPROVED")
                    .containsEntry("refund_minor_units", 18_900L)
                    .containsEntry("rejection_reason", null));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(refundRow(receipt.returnId()))
                    .containsEntry("status", "SCHEDULED")
                    .containsEntry("refund_minor_units", 18_900L));
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(returnCase(receipt.returnId()))
                    .get()
                    .extracting(ReturnCaseView::status)
                    .isEqualTo("REFUND_SCHEDULED"));

    var refundReceipt =
        refundOperations.settle(new SettleRefundCommand(receipt.returnId(), "PSP-ORDER-3003"));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(returnCase(receipt.returnId()))
                    .get()
                    .satisfies(
                        view -> {
                          assertThat(view.status()).isEqualTo("REFUNDED");
                          assertThat(view.refundId()).isEqualTo(refundReceipt.refundId());
                          assertThat(view.refundStatus()).isEqualTo("COMPLETED");
                        }));
    assertThat(refundRow(receipt.returnId()))
        .containsEntry("status", "COMPLETED")
        .containsEntry("provider_reference", "PSP-ORDER-3003")
        .containsEntry("version", 1L);

    assertThat(
            refundOperations.settle(new SettleRefundCommand(receipt.returnId(), "PSP-ORDER-3003")))
        .isEqualTo(refundReceipt);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(publicationCount()).isEqualTo(8);
              assertThat(completedPublicationCount()).isEqualTo(8);
            });

    assertThat(inspectionRow(receipt.returnId()))
        .containsEntry("status", "COMPLETED")
        .containsEntry("outcome", "ACCEPTED")
        .containsEntry("version", 1L);

    assertThatThrownBy(
            () ->
                inspectionWork.complete(
                    new CompleteInspectionCommand(
                        receipt.returnId(), "REJECTED", "Attempted correction.")))
        .isInstanceOf(InspectionAlreadyCompletedException.class);
    assertThat(resolutionCount(receipt.returnId())).isEqualTo(1);
  }

  @Test
  void handlesCompetingDuplicateEventsIdempotently() {
    UUID returnId = UUID.fromString("32e3788c-8027-4ebd-90dd-de73dbfb6750");
    ReturnRequested originalEvent =
        new ReturnRequested(
            UUID.fromString("524f0902-0713-489d-8047-7ddfa76e5b61"),
            returnId,
            "ORDER-REPLAY",
            "LINE-8",
            "WRONG_ITEM",
            7_500,
            "GBP",
            Instant.parse("2026-07-29T10:00:00Z"));
    ReturnRequested duplicateEvent =
        new ReturnRequested(
            UUID.fromString("419411b1-8f6e-4b11-9407-9207a36af019"),
            returnId,
            "ORDER-REPLAY",
            "LINE-8",
            "WRONG_ITEM",
            7_500,
            "GBP",
            Instant.parse("2026-07-29T10:00:01Z"));
    TransactionTemplate transactions = new TransactionTemplate(transactionManager);

    transactions.executeWithoutResult(ignored -> eventPublisher.publishEvent(originalEvent));
    transactions.executeWithoutResult(ignored -> eventPublisher.publishEvent(duplicateEvent));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(inspectionCount(returnId)).isEqualTo(1));
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(returnCase(returnId))
                    .get()
                    .satisfies(
                        view -> {
                          assertThat(view.status()).isEqualTo("REQUESTED");
                          assertThat(view.orderReference()).isEqualTo("ORDER-REPLAY");
                        }));
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              assertThat(publicationCount()).isEqualTo(4);
              assertThat(completedPublicationCount()).isEqualTo(4);
            });
  }

  private Map<String, Object> inspectionRow(UUID returnId) {
    return jdbcTemplate.queryForMap(
        """
        SELECT status, outcome, refund_minor_units, version
          FROM inspection_case
         WHERE return_id = ?
        """,
        returnId);
  }

  private Map<String, Object> resolutionRow(UUID returnId) {
    return jdbcTemplate.queryForMap(
        """
        SELECT status, refund_minor_units, rejection_reason
          FROM return_resolution
         WHERE return_id = ?
        """,
        returnId);
  }

  private Map<String, Object> refundRow(UUID returnId) {
    return jdbcTemplate.queryForMap(
        """
        SELECT status, refund_minor_units, provider_reference, version
          FROM refund_payment
         WHERE return_id = ?
        """,
        returnId);
  }

  private java.util.Optional<ReturnCaseView> returnCase(UUID returnId) {
    return returnCaseQueries.findById(returnId);
  }

  private int inspectionCount(UUID returnId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM inspection_case WHERE return_id = ?", Integer.class, returnId);
  }

  private int resolutionCount(UUID returnId) {
    return jdbcTemplate.queryForObject(
        "SELECT count(*) FROM return_resolution WHERE return_id = ?", Integer.class, returnId);
  }

  private int publicationCount() {
    return jdbcTemplate.queryForObject("SELECT count(*) FROM event_publication", Integer.class);
  }

  private int completedPublicationCount() {
    return jdbcTemplate.queryForObject(
        """
        SELECT count(*)
          FROM event_publication
         WHERE status = 'COMPLETED'
           AND completion_date IS NOT NULL
        """,
        Integer.class);
  }
}
