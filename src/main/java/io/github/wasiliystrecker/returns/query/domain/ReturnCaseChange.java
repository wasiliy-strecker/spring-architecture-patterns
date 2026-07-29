package io.github.wasiliystrecker.returns.query.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Sparse, immutable projection input created from one stable module event. */
public record ReturnCaseChange(
    UUID eventId,
    UUID returnId,
    WorkflowStage stage,
    String orderReference,
    String itemReference,
    String reason,
    Long refundMinorUnits,
    String currency,
    String inspectionOutcome,
    String rejectionReason,
    UUID refundId,
    String refundStatus,
    Instant occurredAt) {

  public ReturnCaseChange {
    Objects.requireNonNull(eventId, "eventId");
    Objects.requireNonNull(returnId, "returnId");
    Objects.requireNonNull(stage, "stage");
    Objects.requireNonNull(occurredAt, "occurredAt");
    if (refundMinorUnits != null && refundMinorUnits <= 0) {
      throw new IllegalArgumentException("refundMinorUnits must be positive");
    }
  }

  public static ReturnCaseChange requested(
      UUID eventId,
      UUID returnId,
      String orderReference,
      String itemReference,
      String reason,
      long refundMinorUnits,
      String currency,
      Instant occurredAt) {
    return new ReturnCaseChange(
        eventId,
        returnId,
        WorkflowStage.REQUESTED,
        orderReference,
        itemReference,
        reason,
        refundMinorUnits,
        currency,
        null,
        null,
        null,
        null,
        occurredAt);
  }

  public static ReturnCaseChange inspected(
      UUID eventId,
      UUID returnId,
      String outcome,
      long refundMinorUnits,
      String currency,
      Instant occurredAt) {
    return new ReturnCaseChange(
        eventId,
        returnId,
        WorkflowStage.INSPECTED,
        null,
        null,
        null,
        refundMinorUnits,
        currency,
        outcome,
        null,
        null,
        null,
        occurredAt);
  }

  public static ReturnCaseChange approved(
      UUID eventId, UUID returnId, long refundMinorUnits, String currency, Instant occurredAt) {
    return new ReturnCaseChange(
        eventId,
        returnId,
        WorkflowStage.APPROVED,
        null,
        null,
        null,
        refundMinorUnits,
        currency,
        null,
        null,
        null,
        null,
        occurredAt);
  }

  public static ReturnCaseChange rejected(
      UUID eventId, UUID returnId, String rejectionReason, Instant occurredAt) {
    return new ReturnCaseChange(
        eventId,
        returnId,
        WorkflowStage.REJECTED,
        null,
        null,
        null,
        null,
        null,
        null,
        rejectionReason,
        null,
        null,
        occurredAt);
  }

  public static ReturnCaseChange refundScheduled(
      UUID eventId,
      UUID returnId,
      UUID refundId,
      long refundMinorUnits,
      String currency,
      Instant occurredAt) {
    return new ReturnCaseChange(
        eventId,
        returnId,
        WorkflowStage.REFUND_SCHEDULED,
        null,
        null,
        null,
        refundMinorUnits,
        currency,
        null,
        null,
        refundId,
        "SCHEDULED",
        occurredAt);
  }

  public static ReturnCaseChange refunded(
      UUID eventId, UUID returnId, UUID refundId, Instant occurredAt) {
    return new ReturnCaseChange(
        eventId,
        returnId,
        WorkflowStage.REFUNDED,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        refundId,
        "COMPLETED",
        occurredAt);
  }
}
