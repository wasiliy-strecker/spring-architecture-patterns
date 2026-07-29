package io.github.wasiliystrecker.returns.inspection.adapter.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SpringDataInspectionCaseRepository extends JpaRepository<InspectionCaseEntity, UUID> {

  boolean existsByReturnIdOrSourceEventId(UUID returnId, UUID sourceEventId);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO inspection_case (
              return_id, source_event_id, status, refund_minor_units,
              refund_currency, registered_at, version
          )
          VALUES (:returnId, :sourceEventId, 'PENDING', :refundMinorUnits,
                  :currency, :registeredAt, 0)
          ON CONFLICT DO NOTHING
          """,
      nativeQuery = true)
  int insertIfAbsent(
      UUID returnId,
      UUID sourceEventId,
      long refundMinorUnits,
      String currency,
      Instant registeredAt);
}
