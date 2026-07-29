package io.github.wasiliystrecker.returns.resolution.application;

import io.github.wasiliystrecker.returns.resolution.domain.ResolutionDecision;
import java.util.UUID;

/** Persistence port owned by the resolution module. */
public interface ResolutionDecisionRepository {

  boolean exists(UUID returnId, UUID sourceEventId);

  boolean addIfAbsent(ResolutionDecision decision);
}
