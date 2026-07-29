package io.github.wasiliystrecker.returns.intake.application;

import io.github.wasiliystrecker.returns.intake.domain.ReturnRequest;

/** Persistence port owned by the intake application layer. */
public interface ReturnRequestRepository {

  boolean exists(String orderReference, String itemReference);

  void add(ReturnRequest request);
}
