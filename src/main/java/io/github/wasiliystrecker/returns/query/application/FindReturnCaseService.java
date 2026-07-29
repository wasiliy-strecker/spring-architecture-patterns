package io.github.wasiliystrecker.returns.query.application;

import io.github.wasiliystrecker.returns.query.ReturnCaseQueries;
import io.github.wasiliystrecker.returns.query.ReturnCaseView;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Framework-independent query use case. */
public final class FindReturnCaseService implements ReturnCaseQueries {
  private final ReturnCaseProjectionRepository projections;

  public FindReturnCaseService(ReturnCaseProjectionRepository projections) {
    this.projections = Objects.requireNonNull(projections, "projections");
  }

  @Override
  public Optional<ReturnCaseView> findById(UUID returnId) {
    return projections.findById(Objects.requireNonNull(returnId, "returnId"));
  }
}
