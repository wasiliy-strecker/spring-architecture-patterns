package io.github.wasiliystrecker.returns.resolution.adapter.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SpringDataResolutionDecisionRepository
    extends JpaRepository<ResolutionDecisionEntity, UUID> {

  boolean existsByReturnIdOrSourceEventId(UUID returnId, UUID sourceEventId);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO return_resolution (
              return_id, source_event_id, decision_event_id, status,
              refund_minor_units, refund_currency, rejection_reason,
              decided_at, version
          )
          VALUES (
              :returnId, :sourceEventId, :decisionEventId, :status,
              :refundMinorUnits, :currency, :rejectionReason,
              :decidedAt, 0
          )
          ON CONFLICT DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      UUID returnId,
      UUID sourceEventId,
      UUID decisionEventId,
      String status,
      long refundMinorUnits,
      String currency,
      String rejectionReason,
      Instant decidedAt);
}
