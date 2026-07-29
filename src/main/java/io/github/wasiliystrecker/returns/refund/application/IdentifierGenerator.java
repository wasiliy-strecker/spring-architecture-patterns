package io.github.wasiliystrecker.returns.refund.application;

import java.util.UUID;

/** Generates refund and event identifiers. */
public interface IdentifierGenerator {

  UUID next();
}
