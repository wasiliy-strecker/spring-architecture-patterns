package io.github.wasiliystrecker.returns.refund.application;

import java.util.function.Supplier;

/** Framework-neutral transaction boundary. */
public interface TransactionRunner {

  <T> T required(Supplier<T> work);
}
