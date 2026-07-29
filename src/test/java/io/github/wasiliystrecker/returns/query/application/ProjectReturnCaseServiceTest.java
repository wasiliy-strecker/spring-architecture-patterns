package io.github.wasiliystrecker.returns.query.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.query.ReturnCaseView;
import io.github.wasiliystrecker.returns.query.domain.ReturnCaseChange;
import io.github.wasiliystrecker.returns.query.domain.WorkflowStage;
import io.github.wasiliystrecker.returns.refund.events.RefundCompleted;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectReturnCaseServiceTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID REFUND_ID = UUID.fromString("cd11bbf1-a2ae-4c01-ac71-bd7fd3018ee2");

  @Test
  void mapsExternalContractsToSparseInternalChanges() {
    CapturingProjections projections = new CapturingProjections();
    ProjectReturnCaseService service = new ProjectReturnCaseService(projections);

    service.project(
        new InspectionCompleted(
            UUID.fromString("a249b0c0-ae77-4847-87b4-9af5428215bb"),
            RETURN_ID,
            "ACCEPTED",
            12_500,
            "EUR",
            Instant.parse("2026-07-29T09:00:00Z")));
    service.project(
        new RefundCompleted(
            UUID.fromString("5413aa62-65ff-41b3-8799-08a6eaf95355"),
            REFUND_ID,
            RETURN_ID,
            "PSP-84729",
            Instant.parse("2026-07-29T11:00:00Z")));

    assertThat(projections.changes)
        .satisfiesExactly(
            inspection -> {
              assertThat(inspection.stage()).isEqualTo(WorkflowStage.INSPECTED);
              assertThat(inspection.inspectionOutcome()).isEqualTo("ACCEPTED");
              assertThat(inspection.refundMinorUnits()).isEqualTo(12_500);
            },
            refunded -> {
              assertThat(refunded.stage()).isEqualTo(WorkflowStage.REFUNDED);
              assertThat(refunded.refundId()).isEqualTo(REFUND_ID);
              assertThat(refunded.refundStatus()).isEqualTo("COMPLETED");
            });
  }

  private static final class CapturingProjections implements ReturnCaseProjectionRepository {
    private final List<ReturnCaseChange> changes = new ArrayList<>();

    @Override
    public void apply(ReturnCaseChange change) {
      changes.add(change);
    }

    @Override
    public Optional<ReturnCaseView> findById(UUID returnId) {
      return Optional.empty();
    }
  }
}
