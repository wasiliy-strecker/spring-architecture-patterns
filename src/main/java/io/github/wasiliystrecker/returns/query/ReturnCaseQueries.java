package io.github.wasiliystrecker.returns.query;

import java.util.Optional;
import java.util.UUID;

/** Public read boundary for eventually consistent return case views. */
public interface ReturnCaseQueries {

  Optional<ReturnCaseView> findById(UUID returnId);
}
