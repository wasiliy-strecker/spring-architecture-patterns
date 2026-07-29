package io.github.wasiliystrecker.returns.refund.adapter;

import io.github.wasiliystrecker.returns.refund.application.TransactionRunner;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

final class SpringTransactionRunner implements TransactionRunner {
  private final TransactionTemplate transactionTemplate;

  SpringTransactionRunner(PlatformTransactionManager transactionManager) {
    this.transactionTemplate =
        new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
  }

  @Override
  public <T> T required(Supplier<T> work) {
    Objects.requireNonNull(work, "work");
    return Objects.requireNonNull(
        transactionTemplate.execute(status -> work.get()), "Transaction returned no result");
  }
}
