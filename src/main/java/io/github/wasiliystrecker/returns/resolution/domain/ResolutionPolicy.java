package io.github.wasiliystrecker.returns.resolution.domain;

import java.util.Locale;

/** Deterministic policy translating an inspection outcome into a final return decision. */
public final class ResolutionPolicy {
  public static final String INSPECTION_FAILED = "INSPECTION_FAILED";

  public ResolutionResult decide(String inspectionOutcome) {
    if (inspectionOutcome == null || inspectionOutcome.isBlank()) {
      throw new InvalidResolutionException("Inspection outcome is required");
    }
    return switch (inspectionOutcome.trim().toUpperCase(Locale.ROOT)) {
      case "ACCEPTED" -> new ResolutionResult(ResolutionStatus.APPROVED, null);
      case "REJECTED" -> new ResolutionResult(ResolutionStatus.REJECTED, INSPECTION_FAILED);
      default ->
          throw new InvalidResolutionException(
              "Unsupported inspection outcome: " + inspectionOutcome);
    };
  }

  /** Result of applying the policy without persistence or framework concerns. */
  public record ResolutionResult(ResolutionStatus status, String rejectionReason) {}
}
