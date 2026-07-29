package io.github.wasiliystrecker.returns.inspection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wasiliystrecker.returns.inspection.CompleteInspectionCommand;
import io.github.wasiliystrecker.returns.inspection.InspectionNotFoundException;
import io.github.wasiliystrecker.returns.inspection.InspectionReceipt;
import io.github.wasiliystrecker.returns.inspection.domain.InspectionCase;
import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class CompleteInspectionServiceTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID INTAKE_EVENT_ID =
      UUID.fromString("a2fe74ac-5fde-4418-8154-e06fdcaed1ea");
  private static final UUID INSPECTION_EVENT_ID =
      UUID.fromString("a249b0c0-ae77-4847-87b4-9af5428215bb");
  private static final Instant REQUESTED_AT = Instant.parse("2026-07-29T08:00:00Z");
  private static final Instant COMPLETED_AT = Instant.parse("2026-07-29T09:00:00Z");

  @Test
  void updatesInspectionAndPublishesOnlyTheStableContract() {
    InMemoryInspections inspections = new InMemoryInspections();
    inspections.inspection =
        InspectionCase.register(RETURN_ID, INTAKE_EVENT_ID, 12_500, "EUR", REQUESTED_AT);
    List<InspectionCompleted> events = new ArrayList<>();
    CompleteInspectionService service = service(inspections, events);

    InspectionReceipt receipt =
        service.complete(
            new CompleteInspectionCommand(RETURN_ID, "accepted", " Item matches request. "));

    assertThat(receipt).isEqualTo(new InspectionReceipt(RETURN_ID, "ACCEPTED", COMPLETED_AT));
    assertThat(inspections.inspection.outcome().name()).isEqualTo("ACCEPTED");
    assertThat(events)
        .containsExactly(
            new InspectionCompleted(
                INSPECTION_EVENT_ID, RETURN_ID, "ACCEPTED", 12_500, "EUR", COMPLETED_AT));
  }

  @Test
  void doesNotPublishWhenInspectionDoesNotExist() {
    InMemoryInspections inspections = new InMemoryInspections();
    List<InspectionCompleted> events = new ArrayList<>();
    CompleteInspectionService service = service(inspections, events);

    assertThatThrownBy(
            () ->
                service.complete(
                    new CompleteInspectionCommand(RETURN_ID, "REJECTED", "Item is damaged")))
        .isInstanceOf(InspectionNotFoundException.class);

    assertThat(events).isEmpty();
  }

  private static CompleteInspectionService service(
      InMemoryInspections inspections, List<InspectionCompleted> events) {
    return new CompleteInspectionService(
        inspections,
        events::add,
        new DirectTransactionRunner(),
        () -> INSPECTION_EVENT_ID,
        Clock.fixed(COMPLETED_AT, ZoneOffset.UTC));
  }

  private static final class InMemoryInspections implements InspectionCaseRepository {
    private InspectionCase inspection;

    @Override
    public boolean exists(UUID returnId, UUID sourceEventId) {
      return inspection != null;
    }

    @Override
    public void add(InspectionCase newInspection) {
      inspection = newInspection;
    }

    @Override
    public Optional<InspectionCase> findByReturnId(UUID returnId) {
      return Optional.ofNullable(inspection);
    }

    @Override
    public void update(InspectionCase completedInspection) {
      inspection = completedInspection;
    }
  }

  private static final class DirectTransactionRunner implements TransactionRunner {

    @Override
    public <T> T required(Supplier<T> work) {
      return work.get();
    }
  }
}
