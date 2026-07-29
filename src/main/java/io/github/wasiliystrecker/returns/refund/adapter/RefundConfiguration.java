package io.github.wasiliystrecker.returns.refund.adapter;

import io.github.wasiliystrecker.returns.refund.RefundOperations;
import io.github.wasiliystrecker.returns.refund.application.IdentifierGenerator;
import io.github.wasiliystrecker.returns.refund.application.RefundEventPublisher;
import io.github.wasiliystrecker.returns.refund.application.RefundPaymentRepository;
import io.github.wasiliystrecker.returns.refund.application.ScheduleRefundService;
import io.github.wasiliystrecker.returns.refund.application.SettleRefundService;
import io.github.wasiliystrecker.returns.refund.application.TransactionRunner;
import java.time.Clock;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
class RefundConfiguration {

  @Bean
  IdentifierGenerator refundIdentifierGenerator() {
    return UUID::randomUUID;
  }

  @Bean
  RefundEventPublisher refundEventPublisher(ApplicationEventPublisher publisher) {
    return new SpringRefundEventPublisher(publisher);
  }

  @Bean
  TransactionRunner refundTransactionRunner(PlatformTransactionManager transactionManager) {
    return new SpringTransactionRunner(transactionManager);
  }

  @Bean
  ScheduleRefundService scheduleRefundService(
      RefundPaymentRepository refunds,
      RefundEventPublisher events,
      IdentifierGenerator identifiers,
      Clock clock) {
    return new ScheduleRefundService(refunds, events, identifiers, clock);
  }

  @Bean
  ReturnApprovedListener returnApprovedListener(ScheduleRefundService service) {
    return new ReturnApprovedListener(service);
  }

  @Bean
  RefundOperations refundOperations(
      RefundPaymentRepository refunds,
      RefundEventPublisher events,
      TransactionRunner transactions,
      IdentifierGenerator identifiers,
      Clock clock) {
    return new SettleRefundService(refunds, events, transactions, identifiers, clock);
  }
}
