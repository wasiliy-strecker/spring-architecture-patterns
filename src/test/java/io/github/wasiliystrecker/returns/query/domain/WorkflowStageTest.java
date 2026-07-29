package io.github.wasiliystrecker.returns.query.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkflowStageTest {

  @Test
  void ranksProgressWithoutInventingAnOrderBetweenFinalDecisions() {
    assertThat(WorkflowStage.INSPECTED.isAfter(WorkflowStage.REQUESTED)).isTrue();
    assertThat(WorkflowStage.REFUNDED.isAfter(WorkflowStage.REFUND_SCHEDULED)).isTrue();
    assertThat(WorkflowStage.APPROVED.isAfter(WorkflowStage.REJECTED)).isFalse();
    assertThat(WorkflowStage.REJECTED.isAfter(WorkflowStage.APPROVED)).isFalse();
  }
}
