package io.github.wasiliystrecker.returns;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Shares one real PostgreSQL container across the module integration tests in this JVM. */
public abstract class PostgresIntegrationTest {
  protected static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18.3-alpine");

  static {
    POSTGRES.start();
  }

  @DynamicPropertySource
  static void postgresProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }
}
