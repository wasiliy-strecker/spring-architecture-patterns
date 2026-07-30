package io.github.wasiliystrecker.returns.operations.application;

import io.github.wasiliystrecker.returns.operations.ResubmitPublicationsCommand;

public interface PublicationResubmitter {

  void resubmit(ResubmitPublicationsCommand command);
}
