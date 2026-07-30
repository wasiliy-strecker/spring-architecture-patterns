package io.github.wasiliystrecker.returns.operations;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Audit-friendly acknowledgement of an accepted recovery request. */
public record ResubmissionReceipt(
    Instant acceptedAt,
    long eligiblePublicationsObserved,
    Duration minAge,
    int maxInFlight,
    int batchSize) {

  public ResubmissionReceipt {
    Objects.requireNonNull(acceptedAt, "acceptedAt");
    Objects.requireNonNull(minAge, "minAge");
    if (eligiblePublicationsObserved < 0) {
      throw new IllegalArgumentException("Eligible publication count cannot be negative");
    }
  }
}
