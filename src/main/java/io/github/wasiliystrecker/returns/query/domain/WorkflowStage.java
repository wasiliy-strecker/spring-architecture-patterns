package io.github.wasiliystrecker.returns.query.domain;

/** Monotonic rank used to protect the read model from delayed events. */
public enum WorkflowStage {
  REQUESTED(10),
  INSPECTED(20),
  APPROVED(30),
  REJECTED(30),
  REFUND_SCHEDULED(40),
  REFUNDED(50);

  private final int rank;

  WorkflowStage(int rank) {
    this.rank = rank;
  }

  public int rank() {
    return rank;
  }

  public boolean isAfter(WorkflowStage other) {
    return rank > other.rank;
  }
}
