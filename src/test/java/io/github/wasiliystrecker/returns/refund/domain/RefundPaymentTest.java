package io.github.wasiliystrecker.returns.refund.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wasiliystrecker.returns.refund.RefundAlreadyCompletedException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefundPaymentTest {
  private static final UUID REFUND_ID = UUID.fromString("cd11bbf1-a2ae-4c01-ac71-bd7fd3018ee2");
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID SOURCE_EVENT_ID =
      UUID.fromString("c799f702-c615-4297-9e84-c9fe61808b41");
  private static final Instant SCHEDULED_AT = Instant.parse("2026-07-29T10:00:00Z");
  private static final Instant SETTLED_AT = Instant.parse("2026-07-29T11:00:00Z");

  @Test
  void settlesAScheduledRefundAndNormalizesProviderReference() {
    RefundPayment scheduled = scheduled();

    RefundPayment settled = scheduled.settle(" PSP-84729 ", SETTLED_AT);

    assertThat(settled.status()).isEqualTo(RefundStatus.COMPLETED);
    assertThat(settled.providerReference()).isEqualTo("PSP-84729");
    assertThat(settled.settledAt()).isEqualTo(SETTLED_AT);
    assertThat(settled.refundMinorUnits()).isEqualTo(12_500);
  }

  @Test
  void treatsTheSameProviderAcknowledgementAsIdempotent() {
    RefundPayment settled = scheduled().settle("PSP-84729", SETTLED_AT);

    assertThat(settled.settle(" PSP-84729 ", SETTLED_AT.plusSeconds(30))).isSameAs(settled);
  }

  @Test
  void refusesToRewriteACompletedSettlement() {
    RefundPayment settled = scheduled().settle("PSP-84729", SETTLED_AT);

    assertThatThrownBy(() -> settled.settle("PSP-DIFFERENT", SETTLED_AT.plusSeconds(30)))
        .isInstanceOf(RefundAlreadyCompletedException.class)
        .hasMessageContaining(RETURN_ID.toString());
  }

  private static RefundPayment scheduled() {
    return RefundPayment.schedule(
        REFUND_ID, RETURN_ID, SOURCE_EVENT_ID, 12_500, "eur", SCHEDULED_AT);
  }
}
