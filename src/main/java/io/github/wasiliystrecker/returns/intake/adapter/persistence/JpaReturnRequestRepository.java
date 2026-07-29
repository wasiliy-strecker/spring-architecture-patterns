package io.github.wasiliystrecker.returns.intake.adapter.persistence;

import io.github.wasiliystrecker.returns.intake.DuplicateReturnRequestException;
import io.github.wasiliystrecker.returns.intake.application.ReturnRequestRepository;
import io.github.wasiliystrecker.returns.intake.domain.ReturnRequest;
import java.util.Objects;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaReturnRequestRepository implements ReturnRequestRepository {
  private static final String DUPLICATE_CONSTRAINT = "return_request_order_item_unique";

  private final SpringDataReturnRequestRepository repository;

  JpaReturnRequestRepository(SpringDataReturnRequestRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  @Override
  public boolean exists(String orderReference, String itemReference) {
    return repository.existsByOrderReferenceAndItemReference(orderReference, itemReference);
  }

  @Override
  public void add(ReturnRequest request) {
    Objects.requireNonNull(request, "request");
    try {
      repository.saveAndFlush(ReturnRequestEntity.from(request));
    } catch (DataIntegrityViolationException exception) {
      if (violatesDuplicateConstraint(exception)) {
        throw new DuplicateReturnRequestException(
            request.orderReference(), request.itemReference(), exception);
      }
      throw exception;
    }
  }

  private static boolean violatesDuplicateConstraint(Throwable failure) {
    Throwable current = failure;
    while (current != null) {
      if (current instanceof ConstraintViolationException constraintViolation
          && DUPLICATE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }
}
