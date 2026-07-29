package io.github.wasiliystrecker.returns.refund.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.refund.RefundReceipt;
import io.github.wasiliystrecker.returns.refund.SettleRefundCommand;
import io.github.wasiliystrecker.returns.refund.domain.RefundPayment;
import io.github.wasiliystrecker.returns.refund.events.RefundCompleted;
import io.github.wasiliystrecker.returns.refund.events.RefundEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class SettleRefundServiceTest {
  private static final UUID REFUND_ID = UUID.fromString("cd11bbf1-a2ae-4c01-ac71-bd7fd3018ee2");
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID SOURCE_EVENT_ID =
      UUID.fromString("c799f702-c615-4297-9e84-c9fe61808b41");
  private static final UUID COMPLETED_EVENT_ID =
      UUID.fromString("5413aa62-65ff-41b3-8799-08a6eaf95355");
  private static final Instant SCHEDULED_AT = Instant.parse("2026-07-29T10:00:00Z");
  private static final Instant SETTLED_AT = Instant.parse("2026-07-29T11:00:00Z");

  @Test
  void recordsSettlementAndPublishesCompletion() {
    InMemoryRefunds refunds = new InMemoryRefunds(scheduled());
    List<RefundEvent> events = new ArrayList<>();
    SettleRefundService service = service(refunds, events);

    RefundReceipt receipt = service.settle(new SettleRefundCommand(RETURN_ID, " PSP-84729 "));

    assertThat(receipt).isEqualTo(new RefundReceipt(REFUND_ID, RETURN_ID, "COMPLETED", SETTLED_AT));
    assertThat(events)
        .containsExactly(
            new RefundCompleted(COMPLETED_EVENT_ID, REFUND_ID, RETURN_ID, "PSP-84729", SETTLED_AT));
  }

  @Test
  void returnsOriginalReceiptWithoutAnotherEventForSameAcknowledgement() {
    RefundPayment settled = scheduled().settle("PSP-84729", SETTLED_AT);
    InMemoryRefunds refunds = new InMemoryRefunds(settled);
    List<RefundEvent> events = new ArrayList<>();
    SettleRefundService service = service(refunds, events);

    RefundReceipt receipt = service.settle(new SettleRefundCommand(RETURN_ID, "PSP-84729"));

    assertThat(receipt).isEqualTo(new RefundReceipt(REFUND_ID, RETURN_ID, "COMPLETED", SETTLED_AT));
    assertThat(refunds.updates).isZero();
    assertThat(events).isEmpty();
  }

  private static SettleRefundService service(InMemoryRefunds refunds, List<RefundEvent> events) {
    return new SettleRefundService(
        refunds,
        events::add,
        new DirectTransactionRunner(),
        () -> COMPLETED_EVENT_ID,
        Clock.fixed(SETTLED_AT, ZoneOffset.UTC));
  }

  private static RefundPayment scheduled() {
    return RefundPayment.schedule(
        REFUND_ID, RETURN_ID, SOURCE_EVENT_ID, 12_500, "EUR", SCHEDULED_AT);
  }

  private static final class InMemoryRefunds implements RefundPaymentRepository {
    private RefundPayment saved;
    private int updates;

    private InMemoryRefunds(RefundPayment saved) {
      this.saved = saved;
    }

    @Override
    public boolean exists(UUID returnId, UUID sourceEventId) {
      return saved != null;
    }

    @Override
    public boolean addIfAbsent(RefundPayment refund) {
      return false;
    }

    @Override
    public Optional<RefundPayment> findByReturnId(UUID returnId) {
      return Optional.ofNullable(saved);
    }

    @Override
    public void update(RefundPayment refund) {
      saved = refund;
      updates++;
    }
  }

  private static final class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T required(Supplier<T> work) {
      return work.get();
    }
  }
}
