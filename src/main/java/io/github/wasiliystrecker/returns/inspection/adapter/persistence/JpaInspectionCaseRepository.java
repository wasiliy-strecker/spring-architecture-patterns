package io.github.wasiliystrecker.returns.inspection.adapter.persistence;

import io.github.wasiliystrecker.returns.inspection.InspectionAlreadyCompletedException;
import io.github.wasiliystrecker.returns.inspection.application.InspectionCaseRepository;
import io.github.wasiliystrecker.returns.inspection.domain.InspectionCase;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionCaseRepository implements InspectionCaseRepository {
  private final SpringDataInspectionCaseRepository repository;

  JpaInspectionCaseRepository(SpringDataInspectionCaseRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  @Override
  public boolean exists(UUID returnId, UUID sourceEventId) {
    return repository.existsByReturnIdOrSourceEventId(returnId, sourceEventId);
  }

  @Override
  public void add(InspectionCase inspection) {
    repository.insertIfAbsent(
        inspection.returnId(),
        inspection.sourceEventId(),
        inspection.refundMinorUnits(),
        inspection.currency(),
        inspection.registeredAt());
  }

  @Override
  public Optional<InspectionCase> findByReturnId(UUID returnId) {
    return repository.findById(returnId).map(InspectionCaseEntity::toDomain);
  }

  @Override
  public void update(InspectionCase inspection) {
    try {
      repository.saveAndFlush(InspectionCaseEntity.existingCase(inspection));
    } catch (ObjectOptimisticLockingFailureException concurrentCompletion) {
      throw new InspectionAlreadyCompletedException(inspection.returnId(), concurrentCompletion);
    }
  }
}
