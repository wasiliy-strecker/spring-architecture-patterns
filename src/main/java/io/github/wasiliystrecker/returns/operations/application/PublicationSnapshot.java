package io.github.wasiliystrecker.returns.operations.application;

import java.time.Instant;

/** Persistence-neutral raw backlog state. */
public record PublicationSnapshot(long incomplete, long failed, Instant oldestPublicationAt) {}
