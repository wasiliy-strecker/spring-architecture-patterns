package io.github.wasiliystrecker.returns.inspection;

/** Public use cases offered by the inspection module. */
public interface InspectionWork {

  InspectionReceipt complete(CompleteInspectionCommand command);
}
