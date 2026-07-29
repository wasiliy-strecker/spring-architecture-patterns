package io.github.wasiliystrecker.returns.intake.adapter;

import io.github.wasiliystrecker.returns.intake.ReturnIntake;
import io.github.wasiliystrecker.returns.intake.application.DomainEventPublisher;
import io.github.wasiliystrecker.returns.intake.application.IdentifierGenerator;
import io.github.wasiliystrecker.returns.intake.application.RequestReturnService;
import io.github.wasiliystrecker.returns.intake.application.ReturnRequestRepository;
import io.github.wasiliystrecker.returns.intake.application.TransactionRunner;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
class IntakeConfiguration {

  @Bean
  IdentifierGenerator returnIdentifierGenerator() {
    return UUID::randomUUID;
  }

  @Bean
  DomainEventPublisher returnDomainEventPublisher(ApplicationEventPublisher publisher) {
    return new SpringDomainEventPublisher(publisher);
  }

  @Bean
  TransactionRunner returnTransactionRunner(PlatformTransactionManager transactionManager) {
    return new SpringTransactionRunner(transactionManager);
  }

  @Bean
  ReturnIntake returnIntake(
      ReturnRequestRepository requests,
      DomainEventPublisher events,
      TransactionRunner transactions,
      IdentifierGenerator identifiers,
      Clock clock) {
    return new RequestReturnService(requests, events, transactions, identifiers, clock);
  }
}
