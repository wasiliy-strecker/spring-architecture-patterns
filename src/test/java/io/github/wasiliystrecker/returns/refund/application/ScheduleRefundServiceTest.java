package io.github.wasiliystrecker.returns.refund.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.refund.domain.RefundPayment;
import io.github.wasiliystrecker.returns.refund.events.RefundEvent;
import io.github.wasiliystrecker.returns.refund.events.RefundScheduled;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ScheduleRefundServiceTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID APPROVAL_EVENT_ID =
      UUID.fromString("c799f702-c615-4297-9e84-c9fe61808b41");
  private static final UUID REFUND_ID = UUID.fromString("cd11bbf1-a2ae-4c01-ac71-bd7fd3018ee2");
  private static final UUID SCHEDULED_EVENT_ID =
      UUID.fromString("5413aa62-65ff-41b3-8799-08a6eaf95355");
  private static final Instant DECIDED_AT = Instant.parse("2026-07-29T09:00:00Z");
  private static final Instant SCHEDULED_AT = Instant.parse("2026-07-29T10:00:00Z");

  @Test
  void createsOneInstructionAndPublishesItsStableContract() {
    InMemoryRefunds refunds = new InMemoryRefunds();
    List<RefundEvent> events = new ArrayList<>();
    ScheduleRefundService service = service(refunds, events);

    service.schedule(approval());

    assertThat(refunds.saved.refundId()).isEqualTo(REFUND_ID);
    assertThat(refunds.saved.returnId()).isEqualTo(RETURN_ID);
    assertThat(events)
        .containsExactly(
            new RefundScheduled(
                SCHEDULED_EVENT_ID, REFUND_ID, RETURN_ID, 12_500, "EUR", SCHEDULED_AT));
  }

  @Test
  void ignoresARepeatedApprovalDelivery() {
    InMemoryRefunds refunds = new InMemoryRefunds();
    List<RefundEvent> events = new ArrayList<>();
    ScheduleRefundService service = service(refunds, events);

    service.schedule(approval());
    service.schedule(approval());

    assertThat(refunds.additions).isEqualTo(1);
    assertThat(events).hasSize(1);
  }

  private static ScheduleRefundService service(InMemoryRefunds refunds, List<RefundEvent> events) {
    return new ScheduleRefundService(
        refunds,
        events::add,
        new SequenceIdentifiers(REFUND_ID, SCHEDULED_EVENT_ID),
        Clock.fixed(SCHEDULED_AT, ZoneOffset.UTC));
  }

  private static ReturnApproved approval() {
    return new ReturnApproved(APPROVAL_EVENT_ID, RETURN_ID, 12_500, "EUR", DECIDED_AT);
  }

  private static final class InMemoryRefunds implements RefundPaymentRepository {
    private RefundPayment saved;
    private int additions;

    @Override
    public boolean exists(UUID returnId, UUID sourceEventId) {
      return saved != null
          && (saved.returnId().equals(returnId) || saved.sourceEventId().equals(sourceEventId));
    }

    @Override
    public boolean addIfAbsent(RefundPayment refund) {
      if (exists(refund.returnId(), refund.sourceEventId())) {
        return false;
      }
      saved = refund;
      additions++;
      return true;
    }

    @Override
    public Optional<RefundPayment> findByReturnId(UUID returnId) {
      return Optional.ofNullable(saved);
    }

    @Override
    public void update(RefundPayment refund) {
      saved = refund;
    }
  }

  private static final class SequenceIdentifiers implements IdentifierGenerator {
    private final Deque<UUID> values;

    private SequenceIdentifiers(UUID... values) {
      this.values = new ArrayDeque<>(List.of(values));
    }

    @Override
    public UUID next() {
      return values.removeFirst();
    }
  }
}
