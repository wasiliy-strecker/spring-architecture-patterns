package io.github.wasiliystrecker.returns.operations.adapter.modulith;

import io.github.wasiliystrecker.returns.operations.ResubmitPublicationsCommand;
import io.github.wasiliystrecker.returns.operations.application.PublicationResubmitter;
import java.util.Objects;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.modulith.events.ResubmissionOptions;
import org.springframework.stereotype.Component;

@Component
final class ModulithPublicationResubmitter implements PublicationResubmitter {

  private final IncompleteEventPublications incompletePublications;

  ModulithPublicationResubmitter(IncompleteEventPublications incompletePublications) {
    this.incompletePublications =
        Objects.requireNonNull(incompletePublications, "incompletePublications");
  }

  @Override
  public void resubmit(ResubmitPublicationsCommand command) {
    ResubmissionOptions options =
        ResubmissionOptions.defaults()
            .withMinAge(command.minAge())
            .withMaxInFlight(command.maxInFlight())
            .withBatchSize(command.batchSize());
    incompletePublications.resubmitIncompletePublications(options);
  }
}
