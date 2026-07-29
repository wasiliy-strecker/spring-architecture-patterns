package io.github.wasiliystrecker.returns.query.adapter;

import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import io.github.wasiliystrecker.returns.query.application.ProjectReturnCaseService;
import io.github.wasiliystrecker.returns.refund.events.RefundCompleted;
import io.github.wasiliystrecker.returns.refund.events.RefundScheduled;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import io.github.wasiliystrecker.returns.resolution.events.ReturnRejected;
import java.util.Objects;
import org.springframework.modulith.events.ApplicationModuleListener;

public class ReturnCaseEventListeners {
  private final ProjectReturnCaseService service;

  ReturnCaseEventListeners(ProjectReturnCaseService service) {
    this.service = Objects.requireNonNull(service, "service");
  }

  @ApplicationModuleListener
  public void on(ReturnRequested event) {
    service.project(event);
  }

  @ApplicationModuleListener
  public void on(InspectionCompleted event) {
    service.project(event);
  }

  @ApplicationModuleListener
  public void on(ReturnApproved event) {
    service.project(event);
  }

  @ApplicationModuleListener
  public void on(ReturnRejected event) {
    service.project(event);
  }

  @ApplicationModuleListener
  public void on(RefundScheduled event) {
    service.project(event);
  }

  @ApplicationModuleListener
  public void on(RefundCompleted event) {
    service.project(event);
  }
}
