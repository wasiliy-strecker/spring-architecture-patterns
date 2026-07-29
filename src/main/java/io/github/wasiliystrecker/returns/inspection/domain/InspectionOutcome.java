package io.github.wasiliystrecker.returns.inspection.domain;

import java.util.Locale;

/** Supported physical inspection outcomes. */
public enum InspectionOutcome {
  ACCEPTED,
  REJECTED;

  public static InspectionOutcome from(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidInspectionException("Inspection outcome is required");
    }
    try {
      return valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new InvalidInspectionException("Unsupported inspection outcome: " + value);
    }
  }
}
