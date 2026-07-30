package io.github.wasiliystrecker.returns.query.adapter.web;

import java.util.UUID;

final class ReturnCaseNotFoundException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  ReturnCaseNotFoundException(UUID returnId) {
    super("Return case %s is not available".formatted(returnId));
  }
}
