package io.github.wasiliystrecker.returns.refund.adapter;

import io.github.wasiliystrecker.returns.refund.application.ScheduleRefundService;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import java.util.Objects;
import org.springframework.modulith.events.ApplicationModuleListener;

public class ReturnApprovedListener {
  private final ScheduleRefundService service;

  ReturnApprovedListener(ScheduleRefundService service) {
    this.service = Objects.requireNonNull(service, "service");
  }

  @ApplicationModuleListener
  public void on(ReturnApproved event) {
    service.schedule(event);
  }
}
