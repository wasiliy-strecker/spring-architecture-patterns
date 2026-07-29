package io.github.wasiliystrecker.returns.query.adapter.persistence;

import io.github.wasiliystrecker.returns.query.ReturnCaseView;
import io.github.wasiliystrecker.returns.query.application.ReturnCaseProjectionRepository;
import io.github.wasiliystrecker.returns.query.domain.ReturnCaseChange;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcReturnCaseProjectionRepository implements ReturnCaseProjectionRepository {
  private final JdbcTemplate jdbcTemplate;

  JdbcReturnCaseProjectionRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  @Transactional
  public void apply(ReturnCaseChange change) {
    Objects.requireNonNull(change, "change");
    int accepted =
        jdbcTemplate.update(
            """
            INSERT INTO return_case_projection_event (
                event_id, return_id, event_type, occurred_at
            )
            VALUES (?, ?, ?, ?)
            ON CONFLICT DO NOTHING
            """,
            change.eventId(),
            change.returnId(),
            change.stage().name(),
            Timestamp.from(change.occurredAt()));
    if (accepted == 0) {
      return;
    }

    jdbcTemplate.update(
        """
        INSERT INTO return_case_view AS current (
            return_id, order_reference, item_reference, reason,
            workflow_stage, workflow_rank, refund_minor_units, refund_currency,
            inspection_outcome, rejection_reason, refund_id, refund_status,
            last_updated_at, projection_version
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
        ON CONFLICT (return_id) DO UPDATE SET
            order_reference =
                COALESCE(current.order_reference, EXCLUDED.order_reference),
            item_reference =
                COALESCE(current.item_reference, EXCLUDED.item_reference),
            reason = COALESCE(current.reason, EXCLUDED.reason),
            workflow_stage = CASE
                WHEN EXCLUDED.workflow_rank > current.workflow_rank
                THEN EXCLUDED.workflow_stage
                ELSE current.workflow_stage
            END,
            workflow_rank =
                GREATEST(current.workflow_rank, EXCLUDED.workflow_rank),
            refund_minor_units =
                COALESCE(current.refund_minor_units, EXCLUDED.refund_minor_units),
            refund_currency =
                COALESCE(current.refund_currency, EXCLUDED.refund_currency),
            inspection_outcome =
                COALESCE(current.inspection_outcome, EXCLUDED.inspection_outcome),
            rejection_reason =
                COALESCE(current.rejection_reason, EXCLUDED.rejection_reason),
            refund_id = COALESCE(current.refund_id, EXCLUDED.refund_id),
            refund_status = CASE
                WHEN EXCLUDED.workflow_rank > current.workflow_rank
                THEN COALESCE(EXCLUDED.refund_status, current.refund_status)
                ELSE COALESCE(current.refund_status, EXCLUDED.refund_status)
            END,
            last_updated_at =
                GREATEST(current.last_updated_at, EXCLUDED.last_updated_at),
            projection_version = current.projection_version + 1
        """,
        change.returnId(),
        change.orderReference(),
        change.itemReference(),
        change.reason(),
        change.stage().name(),
        change.stage().rank(),
        change.refundMinorUnits(),
        change.currency(),
        change.inspectionOutcome(),
        change.rejectionReason(),
        change.refundId(),
        change.refundStatus(),
        Timestamp.from(change.occurredAt()));
  }

  @Override
  public Optional<ReturnCaseView> findById(UUID returnId) {
    return jdbcTemplate
        .query(
            """
            SELECT return_id, order_reference, item_reference, reason,
                   workflow_stage, refund_minor_units, refund_currency,
                   inspection_outcome, rejection_reason, refund_id,
                   refund_status, last_updated_at, projection_version
              FROM return_case_view
             WHERE return_id = ?
               AND order_reference IS NOT NULL
            """,
            JdbcReturnCaseProjectionRepository::mapView,
            returnId)
        .stream()
        .findFirst();
  }

  private static ReturnCaseView mapView(ResultSet result, int rowNumber) throws SQLException {
    return new ReturnCaseView(
        result.getObject("return_id", UUID.class),
        result.getString("order_reference"),
        result.getString("item_reference"),
        result.getString("reason"),
        result.getString("workflow_stage"),
        result.getLong("refund_minor_units"),
        result.getString("refund_currency"),
        result.getString("inspection_outcome"),
        result.getString("rejection_reason"),
        result.getObject("refund_id", UUID.class),
        result.getString("refund_status"),
        result.getTimestamp("last_updated_at").toInstant(),
        result.getLong("projection_version"));
  }
}
