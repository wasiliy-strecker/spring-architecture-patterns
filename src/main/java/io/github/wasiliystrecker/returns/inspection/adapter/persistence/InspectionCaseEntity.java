package io.github.wasiliystrecker.returns.inspection.adapter.persistence;

import io.github.wasiliystrecker.returns.inspection.domain.InspectionCase;
import io.github.wasiliystrecker.returns.inspection.domain.InspectionOutcome;
import io.github.wasiliystrecker.returns.inspection.domain.InspectionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inspection_case")
class InspectionCaseEntity {
  @Id
  @Column(name = "return_id")
  private UUID returnId;

  @Column(name = "source_event_id", nullable = false, unique = true)
  private UUID sourceEventId;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "refund_minor_units", nullable = false)
  private long refundMinorUnits;

  @Column(name = "refund_currency", nullable = false, length = 3)
  private String refundCurrency;

  @Column(length = 16)
  private String outcome;

  @Column(length = 500)
  private String note;

  @Column(name = "registered_at", nullable = false)
  private Instant registeredAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  protected InspectionCaseEntity() {}

  private InspectionCaseEntity(InspectionCase inspection) {
    this.returnId = inspection.returnId();
    this.sourceEventId = inspection.sourceEventId();
    this.status = inspection.status().name();
    this.refundMinorUnits = inspection.refundMinorUnits();
    this.refundCurrency = inspection.currency();
    this.outcome = inspection.outcome() == null ? null : inspection.outcome().name();
    this.note = inspection.note();
    this.registeredAt = inspection.registeredAt();
    this.completedAt = inspection.completedAt();
    this.version = inspection.version();
  }

  static InspectionCaseEntity existingCase(InspectionCase inspection) {
    return new InspectionCaseEntity(inspection);
  }

  InspectionCase toDomain() {
    return new InspectionCase(
        returnId,
        sourceEventId,
        InspectionStatus.valueOf(status),
        refundMinorUnits,
        refundCurrency,
        outcome == null ? null : InspectionOutcome.valueOf(outcome),
        note,
        registeredAt,
        completedAt,
        version);
  }
}
