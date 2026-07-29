package io.github.wasiliystrecker.returns.resolution.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Immutable final decision persisted by the resolution module. */
public record ResolutionDecision(
    UUID returnId,
    UUID sourceEventId,
    UUID decisionEventId,
    ResolutionStatus status,
    long refundMinorUnits,
    String currency,
    String rejectionReason,
    Instant decidedAt,
    long version) {

  public ResolutionDecision {
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(sourceEventId, "sourceEventId");
    Objects.requireNonNull(decisionEventId, "decisionEventId");
    Objects.requireNonNull(status, "status");
    if (refundMinorUnits <= 0) {
      throw new InvalidResolutionException("Refund must be positive");
    }
    if (currency == null || !currency.matches("[A-Za-z]{3}")) {
      throw new InvalidResolutionException("Currency must be a three-letter code");
    }
    currency = currency.toUpperCase(Locale.ROOT);
    Objects.requireNonNull(decidedAt, "decidedAt");
    if (version < 0) {
      throw new InvalidResolutionException("Version cannot be negative");
    }
    if (status == ResolutionStatus.APPROVED && rejectionReason != null) {
      throw new InvalidResolutionException("Approved return cannot have a rejection reason");
    }
    if (status == ResolutionStatus.REJECTED
        && (rejectionReason == null || rejectionReason.isBlank())) {
      throw new InvalidResolutionException("Rejected return requires a reason");
    }
  }

  public static ResolutionDecision approved(
      UUID returnId,
      UUID sourceEventId,
      UUID decisionEventId,
      long refundMinorUnits,
      String currency,
      Instant decidedAt) {
    return new ResolutionDecision(
        returnId,
        sourceEventId,
        decisionEventId,
        ResolutionStatus.APPROVED,
        refundMinorUnits,
        currency,
        null,
        decidedAt,
        0);
  }

  public static ResolutionDecision rejected(
      UUID returnId,
      UUID sourceEventId,
      UUID decisionEventId,
      long refundMinorUnits,
      String currency,
      String rejectionReason,
      Instant decidedAt) {
    return new ResolutionDecision(
        returnId,
        sourceEventId,
        decisionEventId,
        ResolutionStatus.REJECTED,
        refundMinorUnits,
        currency,
        rejectionReason,
        decidedAt,
        0);
  }
}
