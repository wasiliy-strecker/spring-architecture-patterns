package io.github.wasiliystrecker.returns.intake.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReturnRequestTest {
  private static final UUID RETURN_ID = UUID.fromString("42b8df77-0a99-472a-af3b-1f18d9ab37d3");
  private static final Instant REQUESTED_AT = Instant.parse("2026-07-29T05:00:00Z");

  @Test
  void normalizesReferencesAndOptionalComment() {
    ReturnRequest request =
        ReturnRequest.request(
            RETURN_ID,
            " ORDER-1001 ",
            " LINE-2 ",
            ReturnReason.DAMAGED,
            "  Outer packaging was crushed.  ",
            new Money(12_500, "eur"),
            REQUESTED_AT);

    assertThat(request.orderReference()).isEqualTo("ORDER-1001");
    assertThat(request.itemReference()).isEqualTo("LINE-2");
    assertThat(request.comment()).isEqualTo("Outer packaging was crushed.");
    assertThat(request.requestedRefund()).isEqualTo(new Money(12_500, "EUR"));
    assertThat(request.version()).isZero();
  }

  @Test
  void treatsBlankCommentsAsAbsent() {
    ReturnRequest request =
        ReturnRequest.request(
            RETURN_ID,
            "ORDER-1001",
            "LINE-2",
            ReturnReason.WRONG_ITEM,
            "   ",
            new Money(4_990, "GBP"),
            REQUESTED_AT);

    assertThat(request.comment()).isNull();
  }

  @Test
  void rejectsInvalidReferenceLengthsAndOversizedComments() {
    assertThatThrownBy(
            () ->
                ReturnRequest.request(
                    RETURN_ID,
                    "AB",
                    "LINE-2",
                    ReturnReason.DAMAGED,
                    null,
                    new Money(100, "EUR"),
                    REQUESTED_AT))
        .isInstanceOf(InvalidReturnRequestException.class)
        .hasMessageContaining("Order reference");

    assertThatThrownBy(
            () ->
                ReturnRequest.request(
                    RETURN_ID,
                    "ORDER-1001",
                    "LINE-2",
                    ReturnReason.DAMAGED,
                    "x".repeat(501),
                    new Money(100, "EUR"),
                    REQUESTED_AT))
        .isInstanceOf(InvalidReturnRequestException.class)
        .hasMessageContaining("500");
  }

  @Test
  void parsesStableReasonCodes() {
    assertThat(ReturnReason.from("not-as-described")).isEqualTo(ReturnReason.NOT_AS_DESCRIBED);

    assertThatThrownBy(() -> ReturnReason.from("changed-my-mind"))
        .isInstanceOf(InvalidReturnRequestException.class)
        .hasMessageContaining("Unsupported return reason");
  }
}
