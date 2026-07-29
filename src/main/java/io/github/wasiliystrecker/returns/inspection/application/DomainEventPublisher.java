package io.github.wasiliystrecker.returns.inspection.application;

import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;

/** Outbound port for inspection events. */
public interface DomainEventPublisher {

  void publish(InspectionCompleted event);
}
