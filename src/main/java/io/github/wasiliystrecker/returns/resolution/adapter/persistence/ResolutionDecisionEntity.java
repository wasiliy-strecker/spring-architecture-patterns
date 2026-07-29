package io.github.wasiliystrecker.returns.resolution.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_resolution")
class ResolutionDecisionEntity {
  @Id
  @Column(name = "return_id")
  private UUID returnId;

  @Column(name = "source_event_id", nullable = false, unique = true)
  private UUID sourceEventId;

  @Column(name = "decision_event_id", nullable = false, unique = true)
  private UUID decisionEventId;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "refund_minor_units", nullable = false)
  private long refundMinorUnits;

  @Column(name = "refund_currency", nullable = false, length = 3)
  private String refundCurrency;

  @Column(name = "rejection_reason", length = 32)
  private String rejectionReason;

  @Column(name = "decided_at", nullable = false)
  private Instant decidedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  protected ResolutionDecisionEntity() {}
}
