package io.github.wasiliystrecker.returns.refund.adapter.persistence;

import io.github.wasiliystrecker.returns.refund.domain.RefundPayment;
import io.github.wasiliystrecker.returns.refund.domain.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refund_payment")
class RefundPaymentEntity {
  @Id
  @Column(name = "refund_id")
  private UUID refundId;

  @Column(name = "return_id", nullable = false, unique = true)
  private UUID returnId;

  @Column(name = "source_event_id", nullable = false, unique = true)
  private UUID sourceEventId;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "refund_minor_units", nullable = false)
  private long refundMinorUnits;

  @Column(name = "refund_currency", nullable = false, length = 3)
  private String refundCurrency;

  @Column(name = "scheduled_at", nullable = false)
  private Instant scheduledAt;

  @Column(name = "provider_reference", length = 100)
  private String providerReference;

  @Column(name = "settled_at")
  private Instant settledAt;

  @Version
  @Column(nullable = false)
  private Long version;

  protected RefundPaymentEntity() {}

  private RefundPaymentEntity(RefundPayment refund) {
    this.refundId = refund.refundId();
    this.returnId = refund.returnId();
    this.sourceEventId = refund.sourceEventId();
    this.status = refund.status().name();
    this.refundMinorUnits = refund.refundMinorUnits();
    this.refundCurrency = refund.currency();
    this.scheduledAt = refund.scheduledAt();
    this.providerReference = refund.providerReference();
    this.settledAt = refund.settledAt();
    this.version = refund.version();
  }

  static RefundPaymentEntity existing(RefundPayment refund) {
    return new RefundPaymentEntity(refund);
  }

  RefundPayment toDomain() {
    return new RefundPayment(
        refundId,
        returnId,
        sourceEventId,
        RefundStatus.valueOf(status),
        refundMinorUnits,
        refundCurrency,
        scheduledAt,
        providerReference,
        settledAt,
        version);
  }
}
