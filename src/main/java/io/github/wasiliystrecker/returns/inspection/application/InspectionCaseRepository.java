package io.github.wasiliystrecker.returns.inspection.application;

import io.github.wasiliystrecker.returns.inspection.domain.InspectionCase;
import java.util.Optional;
import java.util.UUID;

/** Persistence port owned by the inspection module. */
public interface InspectionCaseRepository {

  boolean exists(UUID returnId, UUID sourceEventId);

  void add(InspectionCase inspection);

  Optional<InspectionCase> findByReturnId(UUID returnId);

  void update(InspectionCase inspection);
}
