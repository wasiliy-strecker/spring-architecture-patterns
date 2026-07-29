package io.github.wasiliystrecker.returns.resolution.adapter;

import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.resolution.application.ResolveInspectionService;
import java.util.Objects;
import org.springframework.modulith.events.ApplicationModuleListener;

final class InspectionCompletedListener {
  private final ResolveInspectionService service;

  InspectionCompletedListener(ResolveInspectionService service) {
    this.service = Objects.requireNonNull(service, "service");
  }

  @ApplicationModuleListener
  void on(InspectionCompleted event) {
    service.resolve(event);
  }
}
