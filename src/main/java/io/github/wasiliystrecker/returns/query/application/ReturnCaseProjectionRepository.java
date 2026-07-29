package io.github.wasiliystrecker.returns.query.application;

import io.github.wasiliystrecker.returns.query.ReturnCaseView;
import io.github.wasiliystrecker.returns.query.domain.ReturnCaseChange;
import java.util.Optional;
import java.util.UUID;

/** Projection persistence port owned by the query module. */
public interface ReturnCaseProjectionRepository {

  void apply(ReturnCaseChange change);

  Optional<ReturnCaseView> findById(UUID returnId);
}
