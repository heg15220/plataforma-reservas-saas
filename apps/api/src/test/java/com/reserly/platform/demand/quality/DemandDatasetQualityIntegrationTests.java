package com.reserly.platform.demand.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

/** Verifica la auditoría agregada y la detección de PII sobre PostgreSQL real. */
@Testcontainers
class DemandDatasetQualityIntegrationTests {
  private static final Instant NOW = Instant.parse("2026-08-14T10:00:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRESQL =
      (PostgreSQLContainer<?>) new ReserlyPostgreSqlContainerProvider().newInstance();

  private static JdbcTemplate jdbc;
  private static DemandDatasetQualityService service;

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
    service = new DemandDatasetQualityService(jdbc, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void reportsOnlyAggregateFindingsAndDetectsPiiInsideAllowedContextKey() {
    insertEvent("{}", UUID.randomUUID());
    insertEvent("{\"approximateZone\":\"person@example.invalid\"}", UUID.randomUUID());

    DemandDatasetQualityReport report = service.audit(Duration.ofHours(24));

    assertThat(report.totalEvents()).isEqualTo(2);
    assertThat(report.piiLeakageEvents()).isOne();
    assertThat(report.incompleteEvents()).isZero();
    assertThat(report.duplicateEvents()).isZero();
    assertThat(report.temporalOrderViolations()).isZero();
    assertThat(report.consentViolations()).isZero();
    assertThat(report.valid()).isFalse();
    assertThat(report.toString()).doesNotContain("person@example.invalid");
  }

  @Test
  void rejectsUnboundedWindowsBeforeQuerying() {
    assertThatThrownBy(() -> service.audit(Duration.ofDays(32)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("31 días");
  }

  private static void insertEvent(String contextJson, UUID eventId) {
    jdbc.update(
        """
        INSERT INTO "BehaviorEvents" (
          "eventId", "schemaVersion", "eventType", "eventFamily", "producer", "purpose",
          "occurredAt", "receivedAt", "requestId", "contextJson", "retentionExpiresAt", "createdAt"
        ) VALUES (?, 1, 'searchPerformed', 'discovery', 'web', 'analytics', ?, ?, ?, ?::jsonb, ?, ?)
        """,
        eventId,
        Timestamp.from(NOW.minusSeconds(120)),
        Timestamp.from(NOW.minusSeconds(60)),
        UUID.randomUUID(),
        contextJson,
        Timestamp.from(NOW.plus(Duration.ofDays(90))),
        Timestamp.from(NOW.minusSeconds(60)));
  }
}
