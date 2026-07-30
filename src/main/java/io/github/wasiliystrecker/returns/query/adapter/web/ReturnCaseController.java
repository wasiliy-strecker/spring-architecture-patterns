package io.github.wasiliystrecker.returns.query.adapter.web;

import io.github.wasiliystrecker.returns.query.ReturnCaseQueries;
import io.github.wasiliystrecker.returns.query.ReturnCaseView;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns/{returnId}")
public final class ReturnCaseController {

  private final ReturnCaseQueries returnCaseQueries;

  ReturnCaseController(ReturnCaseQueries returnCaseQueries) {
    this.returnCaseQueries = returnCaseQueries;
  }

  @GetMapping
  ResponseEntity<ReturnCaseView> find(@PathVariable UUID returnId) {
    return ResponseEntity.ok(
        returnCaseQueries
            .findById(returnId)
            .orElseThrow(() -> new ReturnCaseNotFoundException(returnId)));
  }
}
