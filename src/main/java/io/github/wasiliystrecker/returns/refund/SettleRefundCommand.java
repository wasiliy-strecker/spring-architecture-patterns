package io.github.wasiliystrecker.returns.refund;

import java.util.UUID;

/** Records a successful settlement acknowledged by an external payment provider. */
public record SettleRefundCommand(UUID returnId, String providerReference) {}
