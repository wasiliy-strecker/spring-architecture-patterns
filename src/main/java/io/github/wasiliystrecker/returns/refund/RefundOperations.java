package io.github.wasiliystrecker.returns.refund;

/** Public use cases offered by the refund module. */
public interface RefundOperations {

  RefundReceipt settle(SettleRefundCommand command);
}
