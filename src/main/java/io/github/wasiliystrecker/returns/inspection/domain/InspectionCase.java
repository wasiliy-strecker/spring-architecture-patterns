package io.github.wasiliystrecker.returns.inspection.domain;

import io.github.wasiliystrecker.returns.inspection.InspectionAlreadyCompletedException;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** Immutable aggregate that permits exactly one business completion transition. */
public record InspectionCase(
    UUID returnId,
    UUID sourceEventId,
    InspectionStatus status,
    long refundMinorUnits,
    String currency,
    InspectionOutcome outcome,
    String note,
    Instant registeredAt,
    Instant completedAt,
    long version) {
  private static final int MAX_NOTE_LENGTH = 500;

  public InspectionCase {
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(sourceEventId, "sourceEventId");
    Objects.requireNonNull(status, "status");
    if (refundMinorUnits <= 0) {
      throw new InvalidInspectionException("Refund must be positive");
    }
    if (currency == null || !currency.matches("[A-Za-z]{3}")) {
      throw new InvalidInspectionException("Currency must be a three-letter code");
    }
    currency = currency.toUpperCase(Locale.ROOT);
    note = normalizeNote(note);
    Objects.requireNonNull(registeredAt, "registeredAt");
    if (version < 0) {
      throw new InvalidInspectionException("Version cannot be negative");
    }
    validateState(status, outcome, completedAt);
  }

  public static InspectionCase register(
      UUID returnId,
      UUID sourceEventId,
      long refundMinorUnits,
      String currency,
      Instant registeredAt) {
    return new InspectionCase(
        returnId,
        sourceEventId,
        InspectionStatus.PENDING,
        refundMinorUnits,
        currency,
        null,
        null,
        registeredAt,
        null,
        0);
  }

  public InspectionCase complete(
      InspectionOutcome completedOutcome, String inspectionNote, Instant completionTime) {
    if (status == InspectionStatus.COMPLETED) {
      throw new InspectionAlreadyCompletedException(returnId);
    }
    return new InspectionCase(
        returnId,
        sourceEventId,
        InspectionStatus.COMPLETED,
        refundMinorUnits,
        currency,
        Objects.requireNonNull(completedOutcome, "completedOutcome"),
        inspectionNote,
        registeredAt,
        Objects.requireNonNull(completionTime, "completionTime"),
        version);
  }

  private static void validateState(
      InspectionStatus status, InspectionOutcome outcome, Instant completedAt) {
    if (status == InspectionStatus.PENDING && (outcome != null || completedAt != null)) {
      throw new InvalidInspectionException("Pending inspection cannot contain completion data");
    }
    if (status == InspectionStatus.COMPLETED && (outcome == null || completedAt == null)) {
      throw new InvalidInspectionException("Completed inspection requires outcome and timestamp");
    }
  }

  private static String normalizeNote(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > MAX_NOTE_LENGTH) {
      throw new InvalidInspectionException(
          "Inspection note cannot exceed %d characters".formatted(MAX_NOTE_LENGTH));
    }
    return normalized;
  }
}
