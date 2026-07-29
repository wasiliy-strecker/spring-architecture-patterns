package io.github.wasiliystrecker.returns.inspection.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wasiliystrecker.returns.inspection.InspectionAlreadyCompletedException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InspectionCaseTest {
  private static final UUID RETURN_ID = UUID.fromString("31302ff9-9661-42e4-a87b-2126530422f8");
  private static final UUID SOURCE_EVENT_ID =
      UUID.fromString("a2fe74ac-5fde-4418-8154-e06fdcaed1ea");
  private static final Instant REGISTERED_AT = Instant.parse("2026-07-29T08:00:00Z");
  private static final Instant COMPLETED_AT = Instant.parse("2026-07-29T09:00:00Z");

  @Test
  void completesAPendingInspectionAndNormalizesItsNote() {
    InspectionCase pending =
        InspectionCase.register(RETURN_ID, SOURCE_EVENT_ID, 12_500, "eur", REGISTERED_AT);

    InspectionCase completed =
        pending.complete(
            InspectionOutcome.ACCEPTED, "  Packaging and item verified. ", COMPLETED_AT);

    assertThat(completed.status()).isEqualTo(InspectionStatus.COMPLETED);
    assertThat(completed.outcome()).isEqualTo(InspectionOutcome.ACCEPTED);
    assertThat(completed.note()).isEqualTo("Packaging and item verified.");
    assertThat(completed.completedAt()).isEqualTo(COMPLETED_AT);
    assertThat(completed.refundMinorUnits()).isEqualTo(12_500);
    assertThat(completed.currency()).isEqualTo("EUR");
  }

  @Test
  void refusesASecondCompletion() {
    InspectionCase completed =
        InspectionCase.register(RETURN_ID, SOURCE_EVENT_ID, 12_500, "EUR", REGISTERED_AT)
            .complete(InspectionOutcome.REJECTED, null, COMPLETED_AT);

    assertThatThrownBy(
            () ->
                completed.complete(
                    InspectionOutcome.ACCEPTED,
                    "Changed after review",
                    COMPLETED_AT.plusSeconds(1)))
        .isInstanceOf(InspectionAlreadyCompletedException.class)
        .hasMessageContaining(RETURN_ID.toString());
  }

  @Test
  void validatesOutcomeAndNoteAtTheDomainBoundary() {
    InspectionCase pending =
        InspectionCase.register(RETURN_ID, SOURCE_EVENT_ID, 12_500, "EUR", REGISTERED_AT);

    assertThatThrownBy(() -> InspectionOutcome.from("inconclusive"))
        .isInstanceOf(InvalidInspectionException.class)
        .hasMessageContaining("inconclusive");
    assertThatThrownBy(
            () -> pending.complete(InspectionOutcome.ACCEPTED, "x".repeat(501), COMPLETED_AT))
        .isInstanceOf(InvalidInspectionException.class)
        .hasMessageContaining("500");
  }
}
