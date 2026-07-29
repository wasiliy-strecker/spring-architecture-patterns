package io.github.wasiliystrecker.returns.query.application;

import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import io.github.wasiliystrecker.returns.query.domain.ReturnCaseChange;
import io.github.wasiliystrecker.returns.refund.events.RefundCompleted;
import io.github.wasiliystrecker.returns.refund.events.RefundScheduled;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import io.github.wasiliystrecker.returns.resolution.events.ReturnRejected;
import java.util.Objects;

/** Converts public module events into sparse, monotonic read-model changes. */
public final class ProjectReturnCaseService {
  private final ReturnCaseProjectionRepository projections;

  public ProjectReturnCaseService(ReturnCaseProjectionRepository projections) {
    this.projections = Objects.requireNonNull(projections, "projections");
  }

  public void project(ReturnRequested event) {
    projections.apply(
        ReturnCaseChange.requested(
            event.eventId(),
            event.returnId(),
            event.orderReference(),
            event.itemReference(),
            event.reason(),
            event.refundMinorUnits(),
            event.currency(),
            event.occurredAt()));
  }

  public void project(InspectionCompleted event) {
    projections.apply(
        ReturnCaseChange.inspected(
            event.eventId(),
            event.returnId(),
            event.outcome(),
            event.refundMinorUnits(),
            event.currency(),
            event.completedAt()));
  }

  public void project(ReturnApproved event) {
    projections.apply(
        ReturnCaseChange.approved(
            event.eventId(),
            event.returnId(),
            event.refundMinorUnits(),
            event.currency(),
            event.decidedAt()));
  }

  public void project(ReturnRejected event) {
    projections.apply(
        ReturnCaseChange.rejected(
            event.eventId(), event.returnId(), event.reason(), event.decidedAt()));
  }

  public void project(RefundScheduled event) {
    projections.apply(
        ReturnCaseChange.refundScheduled(
            event.eventId(),
            event.returnId(),
            event.refundId(),
            event.refundMinorUnits(),
            event.currency(),
            event.occurredAt()));
  }

  public void project(RefundCompleted event) {
    projections.apply(
        ReturnCaseChange.refunded(
            event.eventId(), event.returnId(), event.refundId(), event.occurredAt()));
  }
}
