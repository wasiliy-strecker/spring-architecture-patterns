package io.github.wasiliystrecker.returns.inspection.application;

import java.util.UUID;

/** Generates stable identifiers at the application boundary. */
public interface IdentifierGenerator {

  UUID next();
}
