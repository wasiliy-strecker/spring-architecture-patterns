package io.github.wasiliystrecker.returns.refund.adapter;

import io.github.wasiliystrecker.returns.refund.application.RefundEventPublisher;
import io.github.wasiliystrecker.returns.refund.events.RefundEvent;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;

final class SpringRefundEventPublisher implements RefundEventPublisher {
  private final ApplicationEventPublisher publisher;

  SpringRefundEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = Objects.requireNonNull(publisher, "publisher");
  }

  @Override
  public void publish(RefundEvent event) {
    publisher.publishEvent(event);
  }
}
