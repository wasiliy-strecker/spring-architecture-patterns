package io.github.wasiliystrecker.returns.operations.adapter;

import io.github.wasiliystrecker.returns.operations.EventPublicationOperations;
import io.github.wasiliystrecker.returns.operations.application.EventPublicationOperationsService;
import io.github.wasiliystrecker.returns.operations.application.PublicationDiagnostics;
import io.github.wasiliystrecker.returns.operations.application.PublicationResubmitter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OperationsConfiguration {

  @Bean
  EventPublicationOperations eventPublicationOperations(
      PublicationDiagnostics diagnostics, PublicationResubmitter resubmitter, Clock clock) {
    return new EventPublicationOperationsService(diagnostics, resubmitter, clock);
  }
}
