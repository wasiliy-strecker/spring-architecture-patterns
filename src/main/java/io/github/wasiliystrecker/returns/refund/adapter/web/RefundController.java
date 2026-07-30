package io.github.wasiliystrecker.returns.refund.adapter.web;

import io.github.wasiliystrecker.returns.refund.RefundOperations;
import io.github.wasiliystrecker.returns.refund.SettleRefundCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns/{returnId}/refund")
public final class RefundController {

  private final RefundOperations refundOperations;

  RefundController(RefundOperations refundOperations) {
    this.refundOperations = refundOperations;
  }

  @PutMapping
  ResponseEntity<RefundSettledResponse> settle(
      @PathVariable UUID returnId, @Valid @RequestBody SettleRefundRequest request) {
    var receipt =
        refundOperations.settle(new SettleRefundCommand(returnId, request.providerReference()));

    return ResponseEntity.ok(
        new RefundSettledResponse(
            receipt.refundId(), receipt.returnId(), receipt.status(), receipt.settledAt()));
  }

  public record SettleRefundRequest(@NotBlank @Size(min = 3, max = 100) String providerReference) {}

  public record RefundSettledResponse(
      UUID refundId, UUID returnId, String status, Instant settledAt) {}
}
