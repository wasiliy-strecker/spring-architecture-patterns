package io.github.wasiliystrecker.returns.operations.adapter.persistence;

import io.github.wasiliystrecker.returns.operations.application.PublicationDiagnostics;
import io.github.wasiliystrecker.returns.operations.application.PublicationSnapshot;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcPublicationDiagnostics implements PublicationDiagnostics {

  private final JdbcTemplate jdbcTemplate;

  public JdbcPublicationDiagnostics(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public PublicationSnapshot load() {
    return jdbcTemplate.queryForObject(
        """
        SELECT COUNT(*) FILTER (WHERE completion_date IS NULL) AS incomplete,
               COUNT(*) FILTER (
                   WHERE completion_date IS NULL AND status = 'FAILED'
               ) AS failed,
               MIN(publication_date) FILTER (
                   WHERE completion_date IS NULL
               ) AS oldest_publication_at
          FROM event_publication
        """,
        (result, rowNumber) -> {
          Timestamp oldest = result.getTimestamp("oldest_publication_at");
          return new PublicationSnapshot(
              result.getLong("incomplete"),
              result.getLong("failed"),
              oldest == null ? null : oldest.toInstant());
        });
  }

  @Override
  public long countIncompletePublishedBefore(Instant cutoff) {
    Long count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
              FROM event_publication
             WHERE completion_date IS NULL
               AND publication_date <= ?
            """,
            Long.class,
            Timestamp.from(Objects.requireNonNull(cutoff, "cutoff")));
    return Objects.requireNonNull(count, "count");
  }
}
