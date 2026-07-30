package io.github.wasiliystrecker.returns.operations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wasiliystrecker.returns.operations.ResubmitPublicationsCommand;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class EventPublicationOperationsServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-30T08:00:00Z");

  @Test
  void reportsTheBacklogAtAStableObservationTime() {
    var diagnostics =
        new RecordingDiagnostics(
            new PublicationSnapshot(7, 2, NOW.minus(Duration.ofMinutes(12))), 0);
    var resubmitter = new RecordingResubmitter();
    var service =
        new EventPublicationOperationsService(
            diagnostics, resubmitter, Clock.fixed(NOW, ZoneOffset.UTC));

    var backlog = service.status();

    assertThat(backlog.incomplete()).isEqualTo(7);
    assertThat(backlog.failed()).isEqualTo(2);
    assertThat(backlog.oldestPublicationAt()).isEqualTo(NOW.minus(Duration.ofMinutes(12)));
    assertThat(backlog.oldestPublicationAge()).isEqualTo(Duration.ofMinutes(12));
    assertThat(backlog.observedAt()).isEqualTo(NOW);
  }

  @Test
  void acceptsOnlyBoundedRecoveryAndReportsTheObservedEligibleCount() {
    var diagnostics = new RecordingDiagnostics(new PublicationSnapshot(3, 1, NOW), 2);
    var resubmitter = new RecordingResubmitter();
    var service =
        new EventPublicationOperationsService(
            diagnostics, resubmitter, Clock.fixed(NOW, ZoneOffset.UTC));
    var command = new ResubmitPublicationsCommand(Duration.ofMinutes(5), 20, 100);

    var receipt = service.resubmit(command);

    assertThat(diagnostics.lastCutoff).isEqualTo(NOW.minus(Duration.ofMinutes(5)));
    assertThat(resubmitter.lastCommand).isEqualTo(command);
    assertThat(receipt.acceptedAt()).isEqualTo(NOW);
    assertThat(receipt.eligiblePublicationsObserved()).isEqualTo(2);
    assertThat(receipt.minAge()).isEqualTo(Duration.ofMinutes(5));
  }

  @Test
  void rejectsUnsafeRecoveryParameters() {
    assertThatThrownBy(() -> new ResubmitPublicationsCommand(Duration.ofSeconds(29), 20, 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("at least PT30S");
    assertThatThrownBy(() -> new ResubmitPublicationsCommand(Duration.ofMinutes(5), 0, 100))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Max in flight");
    assertThatThrownBy(() -> new ResubmitPublicationsCommand(Duration.ofMinutes(5), 20, 501))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Batch size");
  }

  private static final class RecordingDiagnostics implements PublicationDiagnostics {
    private final PublicationSnapshot snapshot;
    private final long eligible;
    private Instant lastCutoff;

    private RecordingDiagnostics(PublicationSnapshot snapshot, long eligible) {
      this.snapshot = snapshot;
      this.eligible = eligible;
    }

    @Override
    public PublicationSnapshot load() {
      return snapshot;
    }

    @Override
    public long countIncompletePublishedBefore(Instant cutoff) {
      lastCutoff = cutoff;
      return eligible;
    }
  }

  private static final class RecordingResubmitter implements PublicationResubmitter {
    private ResubmitPublicationsCommand lastCommand;

    @Override
    public void resubmit(ResubmitPublicationsCommand command) {
      lastCommand = command;
    }
  }
}
