package io.github.wasiliystrecker.returns.operations;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Point-in-time view of incomplete event publications. */
public record PublicationBacklog(
    long incomplete,
    long failed,
    Instant oldestPublicationAt,
    Duration oldestPublicationAge,
    Instant observedAt) {

  public PublicationBacklog {
    if (incomplete < 0 || failed < 0 || failed > incomplete) {
      throw new IllegalArgumentException("Publication counts are inconsistent");
    }
    if (incomplete == 0 && oldestPublicationAt != null) {
      throw new IllegalArgumentException("An empty backlog cannot have an oldest publication");
    }
    if (incomplete > 0 && oldestPublicationAt == null) {
      throw new IllegalArgumentException("A non-empty backlog requires an oldest publication");
    }
    Objects.requireNonNull(oldestPublicationAge, "oldestPublicationAge");
    Objects.requireNonNull(observedAt, "observedAt");
    if (oldestPublicationAge.isNegative()) {
      throw new IllegalArgumentException("Oldest publication age cannot be negative");
    }
  }
}
