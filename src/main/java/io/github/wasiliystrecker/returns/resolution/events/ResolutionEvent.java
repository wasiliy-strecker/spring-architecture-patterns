package io.github.wasiliystrecker.returns.resolution.events;

import java.time.Instant;
import java.util.UUID;

/** Common contract for decisions published by the resolution module. */
public sealed interface ResolutionEvent permits ReturnApproved, ReturnRejected {

  UUID eventId();

  UUID returnId();

  Instant decidedAt();
}
