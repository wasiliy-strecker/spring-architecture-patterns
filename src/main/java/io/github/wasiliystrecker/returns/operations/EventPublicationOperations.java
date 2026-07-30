package io.github.wasiliystrecker.returns.operations;

/** Operator-facing boundary for diagnosing and recovering durable module events. */
public interface EventPublicationOperations {

  PublicationBacklog status();

  ResubmissionReceipt resubmit(ResubmitPublicationsCommand command);
}
