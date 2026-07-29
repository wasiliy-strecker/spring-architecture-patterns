package io.github.wasiliystrecker.returns.resolution.application;

import java.util.UUID;

/** Generates event identifiers for resolution decisions. */
public interface IdentifierGenerator {

  UUID next();
}
