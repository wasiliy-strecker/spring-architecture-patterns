package io.github.wasiliystrecker.returns.refund.application;

import io.github.wasiliystrecker.returns.refund.events.RefundEvent;

/** Outbound port for refund lifecycle events. */
public interface RefundEventPublisher {

  void publish(RefundEvent event);
}
