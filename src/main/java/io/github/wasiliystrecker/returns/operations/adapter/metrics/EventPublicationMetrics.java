package io.github.wasiliystrecker.returns.operations.adapter.metrics;

import io.github.wasiliystrecker.returns.operations.EventPublicationOperations;
import io.github.wasiliystrecker.returns.operations.PublicationBacklog;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class EventPublicationMetrics implements MeterBinder {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventPublicationMetrics.class);

  private final EventPublicationOperations operations;
  private final AtomicReference<PublicationBacklog> latest =
      new AtomicReference<>(new PublicationBacklog(0, 0, null, Duration.ZERO, Instant.EPOCH));

  EventPublicationMetrics(EventPublicationOperations operations) {
    this.operations = Objects.requireNonNull(operations, "operations");
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    Gauge.builder(
            "returns.event.publications.incomplete",
            latest,
            snapshot -> snapshot.get().incomplete())
        .description("Incomplete durable module event publications")
        .register(registry);
    Gauge.builder("returns.event.publications.failed", latest, snapshot -> snapshot.get().failed())
        .description("Failed durable module event publications")
        .register(registry);
    Gauge.builder(
            "returns.event.publication.oldest.age",
            latest,
            snapshot -> snapshot.get().oldestPublicationAge().toSeconds())
        .baseUnit("seconds")
        .description("Age of the oldest incomplete module event publication")
        .register(registry);
  }

  @EventListener(ApplicationReadyEvent.class)
  void refreshOnStartup() {
    refresh();
  }

  @Scheduled(
      initialDelayString = "${returns.operations.metrics-initial-delay:PT15S}",
      fixedDelayString = "${returns.operations.metrics-refresh-interval:PT15S}")
  void refresh() {
    try {
      latest.set(operations.status());
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Unable to refresh event publication metrics; retaining the previous sample", exception);
    }
  }
}
