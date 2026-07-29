package io.github.wasiliystrecker.returns.intake.application;

import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;

/** Output port for publishing intake events. */
@FunctionalInterface
public interface DomainEventPublisher {

  void publish(ReturnRequested event);
}
