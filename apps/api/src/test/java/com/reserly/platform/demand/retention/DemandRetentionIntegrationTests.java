package com.reserly.platform.demand.retention;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyPostgreSqlContainerProvider;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifica que retención borra solo filas vencidas y respeta el límite de lote. */
@Testcontainers
class DemandRetentionIntegrationTests {
  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRESQL =
      (PostgreSQLContainer<?>) new ReserlyPostgreSqlContainerProvider().newInstance();

  private static JdbcTemplate jdbc;

  @BeforeAll
  static void migrate() {
    Flyway.configure()
        .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    jdbc =
        new JdbcTemplate(
            new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword()));
  }

  @Test
  void removesOnlyExpiredRowsInBoundedRetentionBatch() {
    UUID expiredEvent = UUID.randomUUID();
    UUID activeEvent = UUID.randomUUID();
    insertEvent(expiredEvent, NOW.minusSeconds(1));
    insertEvent(activeEvent, NOW.plus(Duration.ofDays(30)));
    DemandRetentionProperties properties =
        new DemandRetentionProperties(
            10, Duration.ofDays(90), Duration.ofDays(90), 10, 5_000_000, 1_073_741_824);
    DemandRetentionResult result =
        new DemandRetentionService(jdbc, properties, Clock.fixed(NOW, ZoneOffset.UTC)).runOnce();

    assertThat(result.events()).isOne();
    assertThat(count(expiredEvent)).isZero();
    assertThat(count(activeEvent)).isOne();
  }

  private void insertEvent(UUID eventId, Instant retention) {
    Instant occurred = NOW.minus(Duration.ofDays(2));
    Instant received = NOW.minus(Duration.ofDays(1));
    jdbc.update(
        """
        INSERT INTO "BehaviorEvents" (
          "eventId", "schemaVersion", "eventType", "eventFamily", "producer", "purpose",
          "occurredAt", "receivedAt", "requestId", "contextJson", "retentionExpiresAt", "createdAt"
        ) VALUES (?, 1, 'searchPerformed', 'discovery', 'web', 'analytics', ?, ?, ?,
          '{}'::jsonb, ?, ?)
        """,
        eventId,
        Timestamp.from(occurred),
        Timestamp.from(received),
        UUID.randomUUID(),
        Timestamp.from(retention),
        Timestamp.from(received));
  }

  private int count(UUID eventId) {
    Integer result =
        jdbc.queryForObject(
            "SELECT count(*) FROM \"BehaviorEvents\" WHERE \"eventId\" = ?",
            Integer.class,
            eventId);
    return result == null ? 0 : result;
  }
}
