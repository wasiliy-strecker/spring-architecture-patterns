package io.github.wasiliystrecker.returns.intake.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable return request aggregate. */
public record ReturnRequest(
    UUID id,
    String orderReference,
    String itemReference,
    ReturnReason reason,
    String comment,
    Money requestedRefund,
    Instant requestedAt,
    long version) {
  private static final int MIN_REFERENCE_LENGTH = 3;
  private static final int MAX_REFERENCE_LENGTH = 64;
  private static final int MAX_COMMENT_LENGTH = 500;

  public ReturnRequest {
    Objects.requireNonNull(id, "id");
    orderReference = normalizeReference(orderReference, "Order reference");
    itemReference = normalizeReference(itemReference, "Item reference");
    Objects.requireNonNull(reason, "reason");
    comment = normalizeComment(comment);
    Objects.requireNonNull(requestedRefund, "requestedRefund");
    Objects.requireNonNull(requestedAt, "requestedAt");
    if (version < 0) {
      throw new InvalidReturnRequestException("Version cannot be negative");
    }
  }

  public static ReturnRequest request(
      UUID id,
      String orderReference,
      String itemReference,
      ReturnReason reason,
      String comment,
      Money requestedRefund,
      Instant requestedAt) {
    return new ReturnRequest(
        id, orderReference, itemReference, reason, comment, requestedRefund, requestedAt, 0);
  }

  private static String normalizeReference(String value, String label) {
    if (value == null) {
      throw new InvalidReturnRequestException(label + " is required");
    }
    String normalized = value.trim();
    if (normalized.length() < MIN_REFERENCE_LENGTH || normalized.length() > MAX_REFERENCE_LENGTH) {
      throw new InvalidReturnRequestException(
          "%s must contain between %d and %d characters"
              .formatted(label, MIN_REFERENCE_LENGTH, MAX_REFERENCE_LENGTH));
    }
    return normalized;
  }

  private static String normalizeComment(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > MAX_COMMENT_LENGTH) {
      throw new InvalidReturnRequestException(
          "Comment cannot exceed %d characters".formatted(MAX_COMMENT_LENGTH));
    }
    return normalized;
  }
}
