package io.github.wasiliystrecker.returns.intake.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyTest {

  @ParameterizedTest
  @CsvSource({"EUR, EUR", "usd, USD", "' GBP ', GBP"})
  void acceptsAndNormalizesSupportedCurrencies(String currency, String expected) {
    Money money = new Money(12_500, currency);

    assertThat(money.minorUnits()).isEqualTo(12_500);
    assertThat(money.currency()).isEqualTo(expected);
  }

  @Test
  void rejectsZeroAndNegativeAmounts() {
    assertThatThrownBy(() -> new Money(0, "EUR"))
        .isInstanceOf(InvalidReturnRequestException.class)
        .hasMessage("Requested refund must be positive");

    assertThatThrownBy(() -> new Money(-1, "EUR"))
        .isInstanceOf(InvalidReturnRequestException.class);
  }

  @Test
  void rejectsUnsupportedCurrencies() {
    assertThatThrownBy(() -> new Money(100, "JPY"))
        .isInstanceOf(InvalidReturnRequestException.class)
        .hasMessageContaining("Unsupported currency JPY");
  }
}
