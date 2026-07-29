package io.github.wasiliystrecker.returns.inspection;

import java.util.UUID;

/** Command accepted by the inspection module's public use-case boundary. */
public record CompleteInspectionCommand(UUID returnId, String outcome, String note) {}
