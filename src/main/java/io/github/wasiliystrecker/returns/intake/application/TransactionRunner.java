package io.github.wasiliystrecker.returns.intake.application;

import java.util.function.Supplier;

/** Transaction boundary without a dependency on a framework annotation. */
public interface TransactionRunner {

  <T> T required(Supplier<T> work);
}
