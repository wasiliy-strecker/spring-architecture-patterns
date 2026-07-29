package io.github.wasiliystrecker.returns.refund.application;

import io.github.wasiliystrecker.returns.refund.domain.RefundPayment;
import java.util.Optional;
import java.util.UUID;

/** Persistence port owned by the refund module. */
public interface RefundPaymentRepository {

  boolean exists(UUID returnId, UUID sourceEventId);

  boolean addIfAbsent(RefundPayment refund);

  Optional<RefundPayment> findByReturnId(UUID returnId);

  void update(RefundPayment refund);
}
