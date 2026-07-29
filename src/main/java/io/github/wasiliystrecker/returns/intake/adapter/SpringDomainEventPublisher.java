package io.github.wasiliystrecker.returns.intake.adapter;

import io.github.wasiliystrecker.returns.intake.application.DomainEventPublisher;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

final class SpringDomainEventPublisher implements DomainEventPublisher {
  private final ApplicationEventPublisher publisher;

  SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
  }

  @Override
  public void publish(ReturnRequested event) {
    publisher.publishEvent(event);
  }
}
