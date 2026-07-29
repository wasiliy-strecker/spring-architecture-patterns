package io.github.wasiliystrecker.returns.query;

import java.time.Instant;
import java.util.UUID;

/** Read-optimized snapshot assembled from module events. */
public record ReturnCaseView(
    UUID returnId,
    String orderReference,
    String itemReference,
    String reason,
    String status,
    long requestedRefundMinorUnits,
    String currency,
    String inspectionOutcome,
    String rejectionReason,
    UUID refundId,
    String refundStatus,
    Instant lastUpdatedAt,
    long projectionVersion) {}
