package io.github.wasiliystrecker.returns.inspection.application;

import io.github.wasiliystrecker.returns.inspection.domain.InspectionCase;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.util.Objects;

/** Idempotently creates inspection work from the intake event contract. */
public final class RegisterInspectionService {
  private final InspectionCaseRepository inspections;

  public RegisterInspectionService(InspectionCaseRepository inspections) {
    this.inspections = Objects.requireNonNull(inspections, "inspections");
  }

  public void register(ReturnRequested event) {
    Objects.requireNonNull(event, "event");
    if (inspections.exists(event.returnId(), event.eventId())) {
      return;
    }
    inspections.add(
        InspectionCase.register(
            event.returnId(),
            event.eventId(),
            event.refundMinorUnits(),
            event.currency(),
            event.occurredAt()));
  }
}
