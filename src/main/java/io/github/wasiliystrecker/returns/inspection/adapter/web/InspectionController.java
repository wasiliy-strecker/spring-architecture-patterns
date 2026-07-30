package io.github.wasiliystrecker.returns.inspection.adapter.web;

import io.github.wasiliystrecker.returns.inspection.CompleteInspectionCommand;
import io.github.wasiliystrecker.returns.inspection.InspectionWork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/returns/{returnId}/inspection")
public final class InspectionController {

  private final InspectionWork inspectionWork;

  InspectionController(InspectionWork inspectionWork) {
    this.inspectionWork = inspectionWork;
  }

  @PostMapping
  ResponseEntity<InspectionCompletedResponse> complete(
      @PathVariable UUID returnId, @Valid @RequestBody CompleteInspectionRequest request) {
    var receipt =
        inspectionWork.complete(
            new CompleteInspectionCommand(returnId, request.outcome(), request.note()));

    return ResponseEntity.ok(
        new InspectionCompletedResponse(
            receipt.returnId(), receipt.outcome(), receipt.completedAt()));
  }

  public record CompleteInspectionRequest(
      @NotBlank @Pattern(regexp = "(?i)ACCEPTED|REJECTED") String outcome,
      @Size(max = 500) String note) {}

  public record InspectionCompletedResponse(UUID returnId, String outcome, Instant completedAt) {}
}
