package io.github.wasiliystrecker.returns.intake.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.wasiliystrecker.returns.intake.DuplicateReturnRequestException;
import io.github.wasiliystrecker.returns.intake.RequestReturnCommand;
import io.github.wasiliystrecker.returns.intake.ReturnIntake;
import io.github.wasiliystrecker.returns.intake.ReturnReceipt;
import io.github.wasiliystrecker.returns.intake.events.ReturnRequested;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@RecordApplicationEvents
@Import(ReturnIntakePersistenceIT.FailingEventListenerConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
final class ReturnIntakePersistenceIT {

  @Container
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18.3-alpine");

  @Autowired private ReturnIntake returnIntake;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private ApplicationEvents applicationEvents;

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @BeforeEach
  void clearRequests() {
    jdbcTemplate.update("TRUNCATE TABLE return_request");
  }

  @Test
  void persistsAValidatedRequestAndPublishesItsEvent() {
    ReturnReceipt receipt =
        returnIntake.request(
            new RequestReturnCommand(
                " ORDER-1001 ",
                " LINE-2 ",
                "damaged",
                "  Outer packaging was crushed. ",
                12_500,
                "eur"));

    Map<String, Object> row =
        jdbcTemplate.queryForMap(
            """
            SELECT id, order_reference, item_reference, reason, comment,
                   refund_minor_units, refund_currency, version
              FROM return_request
             WHERE id = ?
            """,
            receipt.returnId());

    assertThat(row)
        .containsEntry("id", receipt.returnId())
        .containsEntry("order_reference", "ORDER-1001")
        .containsEntry("item_reference", "LINE-2")
        .containsEntry("reason", "DAMAGED")
        .containsEntry("comment", "Outer packaging was crushed.")
        .containsEntry("refund_minor_units", 12_500L)
        .containsEntry("refund_currency", "EUR")
        .containsEntry("version", 0L);

    assertThat(applicationEvents.stream(ReturnRequested.class).toList())
        .singleElement()
        .satisfies(
            event -> {
              assertThat(event.eventId()).isNotNull();
              assertThat(event.returnId()).isEqualTo(receipt.returnId());
              assertThat(event.orderReference()).isEqualTo("ORDER-1001");
              assertThat(event.itemReference()).isEqualTo("LINE-2");
              assertThat(event.reason()).isEqualTo("DAMAGED");
              assertThat(event.refundMinorUnits()).isEqualTo(12_500);
              assertThat(event.currency()).isEqualTo("EUR");
              assertThat(event.occurredAt()).isEqualTo(receipt.requestedAt());
            });
  }

  @Test
  void preventsASecondReturnForTheSameOrderItem() {
    RequestReturnCommand command =
        new RequestReturnCommand("ORDER-2002", "LINE-7", "WRONG_ITEM", null, 4_990, "GBP");

    returnIntake.request(command);

    assertThatThrownBy(() -> returnIntake.request(command))
        .isInstanceOf(DuplicateReturnRequestException.class);
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                  FROM return_request
                 WHERE order_reference = 'ORDER-2002'
                   AND item_reference = 'LINE-7'
                """,
                Long.class))
        .isEqualTo(1L);
    assertThat(applicationEvents.stream(ReturnRequested.class)).hasSize(1);
  }

  @Test
  void rollsBackPersistenceWhenSynchronousEventPublicationFails() {
    RequestReturnCommand command =
        new RequestReturnCommand("ORDER-ROLLBACK", "LINE-9", "DAMAGED", null, 8_000, "USD");

    assertThatThrownBy(() -> returnIntake.request(command))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Simulated event publication failure");

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM return_request WHERE order_reference = 'ORDER-ROLLBACK'",
                Long.class))
        .isZero();
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FailingEventListenerConfiguration {

    @Bean
    FailingEventListener failingEventListener() {
      return new FailingEventListener();
    }
  }

  static final class FailingEventListener {

    @EventListener
    void on(ReturnRequested event) {
      if (event.orderReference().equals("ORDER-ROLLBACK")) {
        throw new IllegalStateException("Simulated event publication failure");
      }
    }
  }
}
