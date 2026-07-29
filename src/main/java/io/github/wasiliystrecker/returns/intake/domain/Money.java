package io.github.wasiliystrecker.returns.intake.domain;

import java.util.Locale;
import java.util.Set;

/** Positive monetary amount represented without floating-point arithmetic. */
public record Money(long minorUnits, String currency) {
  private static final Set<String> SUPPORTED_CURRENCIES = Set.of("EUR", "USD", "GBP");

  public Money {
    if (minorUnits <= 0) {
      throw new InvalidReturnRequestException("Requested refund must be positive");
    }
    if (currency == null || currency.isBlank()) {
      throw new InvalidReturnRequestException("Currency is required");
    }

    currency = currency.trim().toUpperCase(Locale.ROOT);
    if (!SUPPORTED_CURRENCIES.contains(currency)) {
      throw new InvalidReturnRequestException(
          "Unsupported currency %s; supported currencies are EUR, USD, and GBP"
              .formatted(currency));
    }
  }
}
