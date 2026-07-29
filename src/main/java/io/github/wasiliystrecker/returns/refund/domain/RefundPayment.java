package io.github.wasiliystrecker.returns.refund.domain;

import io.github.wasiliystrecker.returns.refund.RefundAlreadyCompletedException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Immutable aggregate representing one refund instruction per approved return. */
public record RefundPayment(
    UUID refundId,
    UUID returnId,
    UUID sourceEventId,
    RefundStatus status,
    long refundMinorUnits,
    String currency,
    Instant scheduledAt,
    String providerReference,
    Instant settledAt,
    long version) {
  private static final int MIN_PROVIDER_REFERENCE_LENGTH = 3;
  private static final int MAX_PROVIDER_REFERENCE_LENGTH = 100;

  public RefundPayment {
    Objects.requireNonNull(refundId, "refundId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(sourceEventId, "sourceEventId");
    Objects.requireNonNull(status, "status");
    if (refundMinorUnits <= 0) {
      throw new InvalidRefundException("Refund must be positive");
    }
    if (currency == null || !currency.matches("[A-Za-z]{3}")) {
      throw new InvalidRefundException("Currency must be a three-letter code");
    }
    currency = currency.toUpperCase(Locale.ROOT);
    Objects.requireNonNull(scheduledAt, "scheduledAt");
    providerReference = normalizeProviderReference(providerReference);
    if (version < 0) {
      throw new InvalidRefundException("Version cannot be negative");
    }
    validateState(status, providerReference, settledAt);
  }

  public static RefundPayment schedule(
      UUID refundId,
      UUID returnId,
      UUID sourceEventId,
      long refundMinorUnits,
      String currency,
      Instant scheduledAt) {
    return new RefundPayment(
        refundId,
        returnId,
        sourceEventId,
        RefundStatus.SCHEDULED,
        refundMinorUnits,
        currency,
        scheduledAt,
        null,
        null,
        0);
  }

  public RefundPayment settle(String reference, Instant completionTime) {
    String normalizedReference = normalizeProviderReference(reference);
    if (status == RefundStatus.COMPLETED) {
      if (Objects.equals(providerReference, normalizedReference)) {
        return this;
      }
      throw new RefundAlreadyCompletedException(returnId);
    }
    return new RefundPayment(
        refundId,
        returnId,
        sourceEventId,
        RefundStatus.COMPLETED,
        refundMinorUnits,
        currency,
        scheduledAt,
        normalizedReference,
        Objects.requireNonNull(completionTime, "completionTime"),
        version);
  }

  private static void validateState(
      RefundStatus status, String providerReference, Instant settledAt) {
    if (status == RefundStatus.SCHEDULED && (providerReference != null || settledAt != null)) {
      throw new InvalidRefundException("Scheduled refund cannot contain settlement data");
    }
    if (status == RefundStatus.COMPLETED && (providerReference == null || settledAt == null)) {
      throw new InvalidRefundException("Completed refund requires settlement data");
    }
  }

  private static String normalizeProviderReference(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() < MIN_PROVIDER_REFERENCE_LENGTH
        || normalized.length() > MAX_PROVIDER_REFERENCE_LENGTH) {
      throw new InvalidRefundException(
          "Provider reference must contain between %d and %d characters"
              .formatted(MIN_PROVIDER_REFERENCE_LENGTH, MAX_PROVIDER_REFERENCE_LENGTH));
    }
    return normalized;
  }
}
