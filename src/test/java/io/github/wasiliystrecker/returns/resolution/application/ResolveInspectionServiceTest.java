package io.github.wasiliystrecker.returns.resolution.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.resolution.domain.ResolutionDecision;
import io.github.wasiliystrecker.returns.resolution.domain.ResolutionPolicy;
import io.github.wasiliystrecker.returns.resolution.events.ResolutionEvent;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import io.github.wasiliystrecker.returns.resolution.events.ReturnRejected;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolveInspectionServiceTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID INSPECTION_EVENT_ID =
      UUID.fromString("a249b0c0-ae77-4847-87b4-9af5428215bb");
  private static final UUID RESOLUTION_EVENT_ID =
      UUID.fromString("c799f702-c615-4297-9e84-c9fe61808b41");
  private static final Instant COMPLETED_AT = Instant.parse("2026-07-29T09:00:00Z");

  @Test
  void approvesTheFullRefundForAnAcceptedInspection() {
    InMemoryDecisions decisions = new InMemoryDecisions();
    List<ResolutionEvent> events = new ArrayList<>();
    ResolveInspectionService service = service(decisions, events);

    service.resolve(event("ACCEPTED"));

    assertThat(decisions.saved.status().name()).isEqualTo("APPROVED");
    assertThat(events)
        .containsExactly(
            new ReturnApproved(RESOLUTION_EVENT_ID, RETURN_ID, 12_500, "EUR", COMPLETED_AT));
  }

  @Test
  void rejectsAFailedInspectionAndIgnoresItsRedelivery() {
    InMemoryDecisions decisions = new InMemoryDecisions();
    List<ResolutionEvent> events = new ArrayList<>();
    ResolveInspectionService service = service(decisions, events);
    InspectionCompleted event = event("REJECTED");

    service.resolve(event);
    service.resolve(event);

    assertThat(decisions.additions).isEqualTo(1);
    assertThat(events)
        .containsExactly(
            new ReturnRejected(RESOLUTION_EVENT_ID, RETURN_ID, "INSPECTION_FAILED", COMPLETED_AT));
  }

  private static ResolveInspectionService service(
      InMemoryDecisions decisions, List<ResolutionEvent> events) {
    return new ResolveInspectionService(
        decisions, events::add, () -> RESOLUTION_EVENT_ID, new ResolutionPolicy());
  }

  private static InspectionCompleted event(String outcome) {
    return new InspectionCompleted(
        INSPECTION_EVENT_ID, RETURN_ID, outcome, 12_500, "EUR", COMPLETED_AT);
  }

  private static final class InMemoryDecisions implements ResolutionDecisionRepository {
    private ResolutionDecision saved;
    private int additions;

    @Override
    public boolean exists(UUID returnId, UUID sourceEventId) {
      return saved != null
          && (saved.returnId().equals(returnId) || saved.sourceEventId().equals(sourceEventId));
    }

    @Override
    public boolean addIfAbsent(ResolutionDecision decision) {
      if (exists(decision.returnId(), decision.sourceEventId())) {
        return false;
      }
      saved = decision;
      additions++;
      return true;
    }
  }
}
