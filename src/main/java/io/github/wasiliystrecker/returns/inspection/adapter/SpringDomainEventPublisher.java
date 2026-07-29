package io.github.wasiliystrecker.returns.inspection.adapter;

import io.github.wasiliystrecker.returns.inspection.application.DomainEventPublisher;
import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

final class SpringDomainEventPublisher implements DomainEventPublisher {
  private final ApplicationEventPublisher publisher;

  SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
  }

  @Override
  public void publish(InspectionCompleted event) {
    publisher.publishEvent(event);
  }
}
