package io.github.wasiliystrecker.returns.intake.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wasiliystrecker.returns.intake.DuplicateReturnRequestException;
import io.github.wasiliystrecker.returns.intake.RequestReturnCommand;
import io.github.wasiliystrecker.returns.intake.ReturnReceipt;
import io.github.wasiliystrecker.returns.intake.domain.ReturnRequest;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class RequestReturnServiceTest {
  private static final Instant NOW = Instant.parse("2026-07-29T05:00:00Z");
  private static final UUID RETURN_ID = UUID.fromString("42b8df77-0a99-472a-af3b-1f18d9ab37d3");
  private static final UUID EVENT_ID = UUID.fromString("386ea7d3-51c5-4da9-adab-75d55ff55da6");

  @Test
  void storesTheAggregateAndPublishesAStableEvent() {
    InMemoryRequests requests = new InMemoryRequests();
    List<ReturnRequested> events = new ArrayList<>();
    RequestReturnService service = service(requests, events);

    ReturnReceipt receipt =
        service.request(
            new RequestReturnCommand(
                " ORDER-1001 ",
                " LINE-2 ",
                "not-as-described",
                "  Different color. ",
                12_500,
                "eur"));

    assertThat(receipt).isEqualTo(new ReturnReceipt(RETURN_ID, NOW));
    assertThat(requests.saved)
        .singleElement()
        .satisfies(
            request -> {
              assertThat(request.id()).isEqualTo(RETURN_ID);
              assertThat(request.orderReference()).isEqualTo("ORDER-1001");
              assertThat(request.itemReference()).isEqualTo("LINE-2");
              assertThat(request.reason().name()).isEqualTo("NOT_AS_DESCRIBED");
              assertThat(request.comment()).isEqualTo("Different color.");
              assertThat(request.requestedRefund().minorUnits()).isEqualTo(12_500);
              assertThat(request.requestedRefund().currency()).isEqualTo("EUR");
            });
    assertThat(events)
        .containsExactly(
            new ReturnRequested(
                EVENT_ID,
                RETURN_ID,
                "ORDER-1001",
                "LINE-2",
                "NOT_AS_DESCRIBED",
                12_500,
                "EUR",
                NOW));
  }

  @Test
  void rejectsDuplicatesBeforeWritingOrPublishing() {
    InMemoryRequests requests = new InMemoryRequests();
    requests.duplicate = true;
    List<ReturnRequested> events = new ArrayList<>();
    RequestReturnService service = service(requests, events);

    assertThatThrownBy(
            () ->
                service.request(
                    new RequestReturnCommand(
                        "ORDER-1001", "LINE-2", "DAMAGED", null, 1_000, "EUR")))
        .isInstanceOf(DuplicateReturnRequestException.class)
        .hasMessageContaining("ORDER-1001")
        .hasMessageContaining("LINE-2");

    assertThat(requests.saved).isEmpty();
    assertThat(events).isEmpty();
  }

  private static RequestReturnService service(
      InMemoryRequests requests, List<ReturnRequested> events) {
    return new RequestReturnService(
        requests,
        events::add,
        new DirectTransactionRunner(),
        new SequenceIdentifierGenerator(RETURN_ID, EVENT_ID),
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static final class InMemoryRequests implements ReturnRequestRepository {
    private final List<ReturnRequest> saved = new ArrayList<>();
    private boolean duplicate;

    @Override
    public boolean exists(String orderReference, String itemReference) {
      return duplicate;
    }

    @Override
    public void add(ReturnRequest request) {
      saved.add(request);
    }
  }

  private static final class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T required(Supplier<T> work) {
      return work.get();
    }
  }

  private static final class SequenceIdentifierGenerator implements IdentifierGenerator {
    private final Deque<UUID> identifiers;

    private SequenceIdentifierGenerator(UUID... identifiers) {
      this.identifiers = new ArrayDeque<>(List.of(identifiers));
    }

    @Override
    public UUID next() {
      return identifiers.removeFirst();
    }
  }
}
