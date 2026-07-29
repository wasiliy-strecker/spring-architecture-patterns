package io.github.wasiliystrecker.returns.resolution.adapter.persistence;

import io.github.wasiliystrecker.returns.resolution.application.ResolutionDecisionRepository;
import io.github.wasiliystrecker.returns.resolution.domain.ResolutionDecision;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaResolutionDecisionRepository implements ResolutionDecisionRepository {
  private final SpringDataResolutionDecisionRepository repository;

  JpaResolutionDecisionRepository(SpringDataResolutionDecisionRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  @Override
  public boolean exists(UUID returnId, UUID sourceEventId) {
    return repository.existsByReturnIdOrSourceEventId(returnId, sourceEventId);
  }

  @Override
  public boolean addIfAbsent(ResolutionDecision decision) {
    return repository.insertIfAbsent(
            decision.returnId(),
            decision.sourceEventId(),
            decision.decisionEventId(),
            decision.status().name(),
            decision.refundMinorUnits(),
            decision.currency(),
            decision.rejectionReason(),
            decision.decidedAt())
        == 1;
  }
}
