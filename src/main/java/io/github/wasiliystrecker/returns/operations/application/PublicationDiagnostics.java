package io.github.wasiliystrecker.returns.operations.application;

import java.time.Instant;

public interface PublicationDiagnostics {

  PublicationSnapshot load();

  long countIncompletePublishedBefore(Instant cutoff);
}
