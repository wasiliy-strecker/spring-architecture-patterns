package io.github.wasiliystrecker.returns.inspection.adapter;

import io.github.wasiliystrecker.returns.inspection.InspectionWork;
import io.github.wasiliystrecker.returns.inspection.application.CompleteInspectionService;
import io.github.wasiliystrecker.returns.inspection.application.DomainEventPublisher;
import io.github.wasiliystrecker.returns.inspection.application.IdentifierGenerator;
import io.github.wasiliystrecker.returns.inspection.application.InspectionCaseRepository;
import io.github.wasiliystrecker.returns.inspection.application.RegisterInspectionService;
import io.github.wasiliystrecker.returns.inspection.application.TransactionRunner;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
class InspectionConfiguration {

  @Bean
  IdentifierGenerator inspectionIdentifierGenerator() {
    return UUID::randomUUID;
  }

  @Bean
  DomainEventPublisher inspectionDomainEventPublisher(ApplicationEventPublisher publisher) {
    return new SpringDomainEventPublisher(publisher);
  }

  @Bean
  TransactionRunner inspectionTransactionRunner(PlatformTransactionManager transactionManager) {
    return new SpringTransactionRunner(transactionManager);
  }

  @Bean
  RegisterInspectionService registerInspectionService(InspectionCaseRepository inspections) {
    return new RegisterInspectionService(inspections);
  }

  @Bean
  ReturnRequestedListener returnRequestedListener(RegisterInspectionService service) {
    return new ReturnRequestedListener(service);
  }

  @Bean
  InspectionWork inspectionWork(
      InspectionCaseRepository inspections,
      DomainEventPublisher events,
      TransactionRunner transactions,
      IdentifierGenerator identifiers,
      Clock clock) {
    return new CompleteInspectionService(inspections, events, transactions, identifiers, clock);
  }
}
