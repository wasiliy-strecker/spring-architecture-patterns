package io.github.wasiliystrecker.returns.resolution.application;

import io.github.wasiliystrecker.returns.resolution.events.ResolutionEvent;

/** Outbound port for stable resolution event contracts. */
public interface ResolutionEventPublisher {

  void publish(ResolutionEvent event);
}
