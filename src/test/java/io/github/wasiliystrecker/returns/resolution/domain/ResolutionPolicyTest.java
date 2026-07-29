package io.github.wasiliystrecker.returns.resolution.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResolutionPolicyTest {
  private final ResolutionPolicy policy = new ResolutionPolicy();

  @Test
  void approvesAnAcceptedInspection() {
    var result = policy.decide(" accepted ");

    assertThat(result.status()).isEqualTo(ResolutionStatus.APPROVED);
    assertThat(result.rejectionReason()).isNull();
  }

  @Test
  void rejectsAFailedInspectionWithAStableReason() {
    var result = policy.decide("REJECTED");

    assertThat(result.status()).isEqualTo(ResolutionStatus.REJECTED);
    assertThat(result.rejectionReason()).isEqualTo(ResolutionPolicy.INSPECTION_FAILED);
  }

  @Test
  void refusesUnknownInspectionOutcomes() {
    assertThatThrownBy(() -> policy.decide("MANUAL_REVIEW"))
        .isInstanceOf(InvalidResolutionException.class)
        .hasMessageContaining("MANUAL_REVIEW");
  }
}
