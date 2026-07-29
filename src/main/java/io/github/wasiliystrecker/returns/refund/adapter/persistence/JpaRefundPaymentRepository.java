package io.github.wasiliystrecker.returns.refund.adapter.persistence;

import io.github.wasiliystrecker.returns.refund.RefundAlreadyCompletedException;
import io.github.wasiliystrecker.returns.refund.application.RefundPaymentRepository;
import io.github.wasiliystrecker.returns.refund.domain.RefundPayment;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRefundPaymentRepository implements RefundPaymentRepository {
  private final SpringDataRefundPaymentRepository repository;

  JpaRefundPaymentRepository(SpringDataRefundPaymentRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  @Override
  public boolean exists(UUID returnId, UUID sourceEventId) {
    return repository.existsByReturnIdOrSourceEventId(returnId, sourceEventId);
  }

  @Override
  public boolean addIfAbsent(RefundPayment refund) {
    return repository.insertIfAbsent(
            refund.refundId(),
            refund.returnId(),
            refund.sourceEventId(),
            refund.refundMinorUnits(),
            refund.currency(),
            refund.scheduledAt())
        == 1;
  }

  @Override
  public Optional<RefundPayment> findByReturnId(UUID returnId) {
    return repository.findByReturnId(returnId).map(RefundPaymentEntity::toDomain);
  }

  @Override
  public void update(RefundPayment refund) {
    try {
      repository.saveAndFlush(RefundPaymentEntity.existing(refund));
    } catch (ObjectOptimisticLockingFailureException concurrentSettlement) {
      throw new RefundAlreadyCompletedException(refund.returnId(), concurrentSettlement);
    }
  }
}
