package io.github.wasiliystrecker.returns.inspection.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.inspection.domain.InspectionCase;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegisterInspectionServiceTest {

  @Test
  void ignoresARepeatedDeliveryOfTheSameIntakeEvent() {
    InMemoryInspections inspections = new InMemoryInspections();
    RegisterInspectionService service = new RegisterInspectionService(inspections);
    ReturnRequested event =
        new ReturnRequested(
            UUID.fromString("a2fe74ac-5fde-4418-8154-e06fdcaed1ea"),
            UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8"),
            "ORDER-1001",
            "LINE-2",
            "DAMAGED",
            12_500,
            "EUR",
            Instant.parse("2026-07-29T08:00:00Z"));

    service.register(event);
    service.register(event);

    assertThat(inspections.additions).isEqualTo(1);
    assertThat(inspections.inspection.sourceEventId()).isEqualTo(event.eventId());
  }

  private static final class InMemoryInspections implements InspectionCaseRepository {
    private InspectionCase inspection;
    private int additions;

    @Override
    public boolean exists(UUID returnId, UUID sourceEventId) {
      return inspection != null
          && (inspection.returnId().equals(returnId)
              || inspection.sourceEventId().equals(sourceEventId));
    }

    @Override
    public void add(InspectionCase newInspection) {
      inspection = newInspection;
      additions++;
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
}
