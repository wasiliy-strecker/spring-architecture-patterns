package io.github.wasiliystrecker.returns.operations;

import java.time.Duration;
import java.util.Objects;

/** Bounded recovery request for incomplete event publications. */
public record ResubmitPublicationsCommand(Duration minAge, int maxInFlight, int batchSize) {

  private static final Duration MINIMUM_SAFE_AGE = Duration.ofSeconds(30);
  private static final int MAX_IN_FLIGHT_LIMIT = 100;
  private static final int MAX_BATCH_SIZE = 500;

  public ResubmitPublicationsCommand {
    Objects.requireNonNull(minAge, "minAge");
    if (minAge.compareTo(MINIMUM_SAFE_AGE) < 0) {
      throw new IllegalArgumentException(
          "Minimum publication age must be at least " + MINIMUM_SAFE_AGE);
    }
    if (maxInFlight < 1 || maxInFlight > MAX_IN_FLIGHT_LIMIT) {
      throw new IllegalArgumentException(
          "Max in flight must be between 1 and " + MAX_IN_FLIGHT_LIMIT);
    }
    if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
      throw new IllegalArgumentException("Batch size must be between 1 and " + MAX_BATCH_SIZE);
    }
  }
}
