package io.github.wasiliystrecker.returns.intake.adapter.web;

import io.github.wasiliystrecker.returns.intake.RequestReturnCommand;
import io.github.wasiliystrecker.returns.intake.ReturnIntake;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns")
public final class ReturnIntakeController {

  private final ReturnIntake returnIntake;

  ReturnIntakeController(ReturnIntake returnIntake) {
    this.returnIntake = returnIntake;
  }

  @PostMapping
  ResponseEntity<ReturnAcceptedResponse> requestReturn(
      @Valid @RequestBody CreateReturnRequest request) {
    var receipt =
        returnIntake.request(
            new RequestReturnCommand(
                request.orderReference(),
                request.itemReference(),
                request.reason(),
                request.comment(),
                request.requestedRefundMinorUnits(),
                request.currency()));

    return ResponseEntity.created(URI.create("/api/v1/returns/" + receipt.returnId()))
        .body(new ReturnAcceptedResponse(receipt.returnId(), receipt.requestedAt()));
  }

  public record CreateReturnRequest(
      @NotBlank @Size(min = 3, max = 64) String orderReference,
      @NotBlank @Size(min = 3, max = 64) String itemReference,
      @NotBlank String reason,
      @Size(max = 500) String comment,
      @Positive long requestedRefundMinorUnits,
      @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency) {}

  public record ReturnAcceptedResponse(UUID returnId, Instant requestedAt) {}
}
