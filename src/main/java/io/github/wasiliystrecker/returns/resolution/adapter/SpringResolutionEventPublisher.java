package io.github.wasiliystrecker.returns.resolution.adapter;

import io.github.wasiliystrecker.returns.resolution.application.ResolutionEventPublisher;
import io.github.wasiliystrecker.returns.resolution.events.ResolutionEvent;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

final class SpringResolutionEventPublisher implements ResolutionEventPublisher {
  private final ApplicationEventPublisher publisher;

  SpringResolutionEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
  }

  @Override
  public void publish(ResolutionEvent event) {
    publisher.publishEvent(event);
  }
}
