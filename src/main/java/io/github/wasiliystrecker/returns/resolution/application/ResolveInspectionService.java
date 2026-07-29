package io.github.wasiliystrecker.returns.resolution.application;

import io.github.wasiliystrecker.returns.inspection.events.InspectionCompleted;
import io.github.wasiliystrecker.returns.resolution.domain.ResolutionDecision;
import io.github.wasiliystrecker.returns.resolution.domain.ResolutionPolicy;
import io.github.wasiliystrecker.returns.resolution.domain.ResolutionStatus;
import io.github.wasiliystrecker.returns.resolution.events.ResolutionEvent;
import io.github.wasiliystrecker.returns.resolution.events.ReturnApproved;
import io.github.wasiliystrecker.returns.resolution.events.ReturnRejected;
import java.util.Objects;

/** Idempotently applies the resolution policy to completed inspections. */
public final class ResolveInspectionService {
  private final ResolutionDecisionRepository decisions;
  private final ResolutionEventPublisher events;
  private final IdentifierGenerator identifiers;
  private final ResolutionPolicy policy;

  public ResolveInspectionService(
      ResolutionDecisionRepository decisions,
      ResolutionEventPublisher events,
      IdentifierGenerator identifiers,
      ResolutionPolicy policy) {
    this.decisions = Objects.requireNonNull(decisions, "decisions");
    this.events = Objects.requireNonNull(events, "events");
    this.identifiers = Objects.requireNonNull(identifiers, "identifiers");
    this.policy = Objects.requireNonNull(policy, "policy");
  }

  public void resolve(InspectionCompleted event) {
    Objects.requireNonNull(event, "event");
    if (decisions.exists(event.returnId(), event.eventId())) {
      return;
    }

    var result = policy.decide(event.outcome());
    var decision = createDecision(event, result);
    if (decisions.addIfAbsent(decision)) {
      events.publish(toEvent(decision));
    }
  }

  private ResolutionDecision createDecision(
      InspectionCompleted event, ResolutionPolicy.ResolutionResult result) {
    if (result.status() == ResolutionStatus.APPROVED) {
      return ResolutionDecision.approved(
          event.returnId(),
          event.eventId(),
          identifiers.next(),
          event.refundMinorUnits(),
          event.currency(),
          event.completedAt());
    }
    return ResolutionDecision.rejected(
        event.returnId(),
        event.eventId(),
        identifiers.next(),
        event.refundMinorUnits(),
        event.currency(),
        result.rejectionReason(),
        event.completedAt());
  }

  private static ResolutionEvent toEvent(ResolutionDecision decision) {
    if (decision.status() == ResolutionStatus.APPROVED) {
      return new ReturnApproved(
          decision.decisionEventId(),
          decision.returnId(),
          decision.refundMinorUnits(),
          decision.currency(),
          decision.decidedAt());
    }
    return new ReturnRejected(
        decision.decisionEventId(),
        decision.returnId(),
        decision.rejectionReason(),
        decision.decidedAt());
  }
}
