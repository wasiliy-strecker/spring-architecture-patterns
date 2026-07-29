package io.github.wasiliystrecker.returns.intake.application;

import java.util.UUID;

/** Output port for deterministic identifier generation in tests. */
@FunctionalInterface
public interface IdentifierGenerator {

  UUID next();
}
