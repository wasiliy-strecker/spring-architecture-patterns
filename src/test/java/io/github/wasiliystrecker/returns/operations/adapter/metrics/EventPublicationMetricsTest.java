package io.github.wasiliystrecker.returns.operations.adapter.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.operations.EventPublicationOperations;
import io.github.wasiliystrecker.returns.operations.PublicationBacklog;
import io.github.wasiliystrecker.returns.operations.ResubmissionReceipt;
import io.github.wasiliystrecker.returns.operations.ResubmitPublicationsCommand;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class EventPublicationMetricsTest {

  @Test
  void exposesOneCoherentSampleWithoutQueryingOnEveryGaugeRead() {
    var operations = new StubOperations();
    var metrics = new EventPublicationMetrics(operations);
    var registry = new SimpleMeterRegistry();
    metrics.bindTo(registry);

    metrics.refresh();

    assertThat(registry.get("returns.event.publications.incomplete").gauge().value()).isEqualTo(4);
    assertThat(registry.get("returns.event.publications.failed").gauge().value()).isEqualTo(1);
    assertThat(registry.get("returns.event.publication.oldest.age").gauge().value()).isEqualTo(90);
    assertThat(operations.statusCalls).isEqualTo(1);
  }

  private static final class StubOperations implements EventPublicationOperations {
    private int statusCalls;

    @Override
    public PublicationBacklog status() {
      statusCalls++;
      return new PublicationBacklog(
          4,
          1,
          Instant.parse("2026-07-30T07:58:30Z"),
          Duration.ofSeconds(90),
          Instant.parse("2026-07-30T08:00:00Z"));
    }

    @Override
    public ResubmissionReceipt resubmit(ResubmitPublicationsCommand command) {
      throw new UnsupportedOperationException("Not used by this test");
    }
  }
}
