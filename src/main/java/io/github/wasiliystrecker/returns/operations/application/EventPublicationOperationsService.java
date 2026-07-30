package io.github.wasiliystrecker.returns.operations.application;

import io.github.wasiliystrecker.returns.operations.EventPublicationOperations;
import io.github.wasiliystrecker.returns.operations.PublicationBacklog;
import io.github.wasiliystrecker.returns.operations.ResubmissionReceipt;
import io.github.wasiliystrecker.returns.operations.ResubmitPublicationsCommand;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class EventPublicationOperationsService implements EventPublicationOperations {

  private final PublicationDiagnostics diagnostics;
  private final PublicationResubmitter resubmitter;
  private final Clock clock;

  public EventPublicationOperationsService(
      PublicationDiagnostics diagnostics, PublicationResubmitter resubmitter, Clock clock) {
    this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    this.resubmitter = Objects.requireNonNull(resubmitter, "resubmitter");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public PublicationBacklog status() {
    Instant observedAt = clock.instant();
    PublicationSnapshot snapshot = diagnostics.load();
    Duration oldestAge =
        snapshot.oldestPublicationAt() == null
            ? Duration.ZERO
            : nonNegativeAge(snapshot.oldestPublicationAt(), observedAt);

    return new PublicationBacklog(
        snapshot.incomplete(),
        snapshot.failed(),
        snapshot.oldestPublicationAt(),
        oldestAge,
        observedAt);
  }

  @Override
  public ResubmissionReceipt resubmit(ResubmitPublicationsCommand command) {
    Objects.requireNonNull(command, "command");
    Instant acceptedAt = clock.instant();
    long eligible = diagnostics.countIncompletePublishedBefore(acceptedAt.minus(command.minAge()));

    resubmitter.resubmit(command);

    return new ResubmissionReceipt(
        acceptedAt, eligible, command.minAge(), command.maxInFlight(), command.batchSize());
  }

  private static Duration nonNegativeAge(Instant publicationAt, Instant observedAt) {
    Duration age = Duration.between(publicationAt, observedAt);
    return age.isNegative() ? Duration.ZERO : age;
  }
}
