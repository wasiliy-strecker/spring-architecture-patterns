package io.github.wasiliystrecker.returns.inspection;

import java.time.Instant;
import java.util.UUID;

/** Confirmation returned after an inspection has been completed. */
public record InspectionReceipt(UUID returnId, String outcome, Instant completedAt) {}
