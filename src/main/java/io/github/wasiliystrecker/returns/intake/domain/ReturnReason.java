package io.github.wasiliystrecker.returns.intake.domain;

import java.util.Locale;

/** Supported reasons for entering the returns workflow. */
public enum ReturnReason {
  DAMAGED,
  NOT_AS_DESCRIBED,
  WRONG_ITEM,
  NO_LONGER_NEEDED;

  public static ReturnReason from(String value) {
    if (value == null || value.isBlank()) {
      throw new InvalidReturnRequestException("Return reason is required");
    }

    String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    try {
      return valueOf(normalized);
    } catch (IllegalArgumentException exception) {
      throw new InvalidReturnRequestException("Unsupported return reason: " + value);
    }
  }
}
