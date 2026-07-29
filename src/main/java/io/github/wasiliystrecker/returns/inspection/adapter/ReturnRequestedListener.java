package io.github.wasiliystrecker.returns.inspection.adapter;

import io.github.wasiliystrecker.returns.inspection.application.RegisterInspectionService;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.util.Objects;
import org.springframework.modulith.events.ApplicationModuleListener;

public class ReturnRequestedListener {
  private final RegisterInspectionService service;

  ReturnRequestedListener(RegisterInspectionService service) {
    this.service = Objects.requireNonNull(service, "service");
  }

  @ApplicationModuleListener
  public void on(ReturnRequested event) {
    service.register(event);
  }
}
