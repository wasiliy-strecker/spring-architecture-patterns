package io.github.wasiliystrecker.returns.refund.events;

import java.time.Instant;
import java.util.UUID;

/** Common contract for lifecycle events published by the refund module. */
public sealed interface RefundEvent permits RefundScheduled, RefundCompleted {

  UUID eventId();

  UUID refundId();

  UUID returnId();

  Instant occurredAt();
}
