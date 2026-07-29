package io.github.wasiliystrecker.returns.intake.adapter.persistence;

import io.github.wasiliystrecker.returns.intake.domain.ReturnRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_request")
class ReturnRequestEntity {
  @Id private UUID id;

  @Column(name = "order_reference", nullable = false, length = 64)
  private String orderReference;

  @Column(name = "item_reference", nullable = false, length = 64)
  private String itemReference;

  @Column(nullable = false, length = 32)
  private String reason;

  @Column(length = 500)
  private String comment;

  @Column(name = "refund_minor_units", nullable = false)
  private long refundMinorUnits;

  @Column(name = "refund_currency", nullable = false, length = 3)
  private String refundCurrency;

  @Column(name = "requested_at", nullable = false)
  private Instant requestedAt;

  @Version
  @Column(nullable = false)
  private Long version;

  protected ReturnRequestEntity() {}

  private ReturnRequestEntity(ReturnRequest request) {
    this.id = request.id();
    this.orderReference = request.orderReference();
    this.itemReference = request.itemReference();
    this.reason = request.reason().name();
    this.comment = request.comment();
    this.refundMinorUnits = request.requestedRefund().minorUnits();
    this.refundCurrency = request.requestedRefund().currency();
    this.requestedAt = request.requestedAt();
    this.version = request.version() == 0 ? null : request.version();
  }

  static ReturnRequestEntity from(ReturnRequest request) {
    return new ReturnRequestEntity(request);
  }
}
