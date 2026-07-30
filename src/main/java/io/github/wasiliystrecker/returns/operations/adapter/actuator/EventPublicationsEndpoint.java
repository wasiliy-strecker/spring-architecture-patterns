package io.github.wasiliystrecker.returns.operations.adapter.actuator;

import io.github.wasiliystrecker.returns.operations.EventPublicationOperations;
import io.github.wasiliystrecker.returns.operations.PublicationBacklog;
import io.github.wasiliystrecker.returns.operations.ResubmissionReceipt;
import io.github.wasiliystrecker.returns.operations.ResubmitPublicationsCommand;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "eventpublications")
public final class EventPublicationsEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(EventPublicationsEndpoint.class);
  private static final Duration DEFAULT_MIN_AGE = Duration.ofMinutes(5);
  private static final int DEFAULT_MAX_IN_FLIGHT = 20;
  private static final int DEFAULT_BATCH_SIZE = 100;

  private final EventPublicationOperations operations;

  EventPublicationsEndpoint(EventPublicationOperations operations) {
    this.operations = Objects.requireNonNull(operations, "operations");
  }

  @ReadOperation
  public PublicationBacklog status() {
    return operations.status();
  }

  @WriteOperation
  public ResubmissionReceipt resubmit(Duration minAge, Integer maxInFlight, Integer batchSize) {
    var command =
        new ResubmitPublicationsCommand(
            minAge == null ? DEFAULT_MIN_AGE : minAge,
            maxInFlight == null ? DEFAULT_MAX_IN_FLIGHT : maxInFlight,
            batchSize == null ? DEFAULT_BATCH_SIZE : batchSize);
    ResubmissionReceipt receipt = operations.resubmit(command);

    LOGGER.info(
        "Event publication recovery accepted principal={} eligible={} minAge={} "
            + "maxInFlight={} batchSize={}",
        authenticatedSubject(),
        receipt.eligiblePublicationsObserved(),
        receipt.minAge(),
        receipt.maxInFlight(),
        receipt.batchSize());

    return receipt;
  }

  private static String authenticatedSubject() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return "unknown";
    }
    return authentication.getName().replace('\r', '_').replace('\n', '_');
  }
}
