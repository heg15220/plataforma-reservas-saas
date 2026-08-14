package com.reserly.platform.demand.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyPostgreSqlContainerProvider;
import com.reserly.platform.demand.quality.DemandDatasetQualityService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

/** Verifica volumen, métricas runtime, latencia y cobertura sin dimensiones personales. */
@Testcontainers
class DemandObservabilityIntegrationTests {
  private static final Instant NOW = Instant.parse("2026-08-14T11:00:00Z");

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
  void buildsPerEventVersionDashboardAndDeclaresMetricScope() {
    insertEvent();
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    Counter.builder("reserly.demand.events.outcomes")
        .tags("eventType", "searchPerformed", "schemaVersion", "1", "result", "accepted")
        .register(meters)
        .increment();
    Timer.builder("reserly.demand.events.latency")
        .tags(
            "eventType",
            "searchPerformed",
            "schemaVersion",
            "1",
            "phase",
            "storage",
            "result",
            "accepted")
        .register(meters)
        .record(Duration.ofMillis(12));
    Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    DemandObservabilityService service =
        new DemandObservabilityService(
            jdbc, meters, new DemandDatasetQualityService(jdbc, clock), clock);

    DemandObservabilityDashboard dashboard = service.dashboard(Duration.ofHours(24));

    DemandEventMetric search =
        dashboard.events().stream()
            .filter(metric -> metric.eventType().equals("searchPerformed"))
            .findFirst()
            .orElseThrow();
    assertThat(search.persistedVolume()).isOne();
    assertThat(search.accepted()).isOne();
    assertThat(search.latencySamples()).isOne();
    assertThat(search.meanLatencyMilliseconds()).isEqualTo(12.0);
    assertThat(search.covered()).isTrue();
    assertThat(dashboard.totalPersistedVolume()).isOne();
    assertThat(dashboard.runtimeCounterScope()).isEqualTo("process_lifetime");
    assertThat(dashboard.missingEventTypes())
        .contains("bookingCompleted")
        .doesNotContain("searchPerformed");
    assertThat(dashboard.instrumentationCoveragePercent()).isBetween(0.0, 100.0);
    assertThat(dashboard.quality().valid()).isTrue();
  }

  private static void insertEvent() {
    Instant receivedAt = NOW.minusSeconds(60);
    jdbc.update(
        """
        INSERT INTO "BehaviorEvents" (
          "eventId", "schemaVersion", "eventType", "eventFamily", "producer", "purpose",
          "occurredAt", "receivedAt", "requestId", "contextJson", "retentionExpiresAt", "createdAt"
        ) VALUES (?, 1, 'searchPerformed', 'discovery', 'web', 'analytics', ?, ?, ?,
          '{}'::jsonb, ?, ?)
        """,
        UUID.randomUUID(),
        Timestamp.from(receivedAt.minusSeconds(1)),
        Timestamp.from(receivedAt),
        UUID.randomUUID(),
        Timestamp.from(NOW.plus(Duration.ofDays(90))),
        Timestamp.from(receivedAt));
  }
}
