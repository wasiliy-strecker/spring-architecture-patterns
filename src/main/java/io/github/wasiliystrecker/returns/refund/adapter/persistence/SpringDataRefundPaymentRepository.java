package io.github.wasiliystrecker.returns.refund.adapter.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SpringDataRefundPaymentRepository extends JpaRepository<RefundPaymentEntity, UUID> {

  boolean existsByReturnIdOrSourceEventId(UUID returnId, UUID sourceEventId);

  Optional<RefundPaymentEntity> findByReturnId(UUID returnId);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO refund_payment (
              refund_id, return_id, source_event_id, status,
              refund_minor_units, refund_currency, scheduled_at, version
          )
          VALUES (
              :refundId, :returnId, :sourceEventId, 'SCHEDULED',
              :refundMinorUnits, :currency, :scheduledAt, 0
          )
          ON CONFLICT DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      UUID refundId,
      UUID returnId,
      UUID sourceEventId,
      long refundMinorUnits,
      String currency,
      Instant scheduledAt);
}
