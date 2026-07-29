package io.github.wasiliystrecker.returns.refund;

import java.time.Instant;
import java.util.UUID;

/** Confirmation returned for an idempotently recorded refund settlement. */
public record RefundReceipt(UUID refundId, UUID returnId, String status, Instant settledAt) {}
