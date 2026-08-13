package com.reserly.platform.demand.privacy;

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
import tools.jackson.databind.ObjectMapper;

/** Verifica derechos, cascadas e idempotencia sobre PostgreSQL real. */
@Testcontainers
class DemandPrivacyIntegrationTests {
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
  void erasesIdentityEventsAndDerivedRequestsIdempotently() {
    UUID identityId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    insertCustomer(identityId);
    insertEvent(eventId, identityId, NOW.plus(Duration.ofDays(30)));
    DemandPrivacyService service =
        new DemandPrivacyService(jdbc, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
    DemandPrivacyRequest request =
        new DemandPrivacyRequest(
            UUID.randomUUID(), identityId, "customer", "erasure", null, null, null);

    DemandPrivacyResponse first = service.execute(request);
    DemandPrivacyResponse retry = service.execute(request);

    assertThat(first.status()).isEqualTo("completed");
    assertThat(first.result())
        .containsEntry("eventsDeleted", 1)
        .containsEntry("profilesDeleted", 0)
        .containsEntry("identityDeleted", true);
    assertThat(retry).isEqualTo(first);
    assertThat(count("CustomerIdentities", "id", identityId)).isZero();
    assertThat(count("BehaviorEvents", "eventId", eventId)).isZero();
    assertThat(count("DemandPrivacyRequests", "requestId", request.requestId())).isOne();
  }

  private void insertCustomer(UUID id) {
    jdbc.update(
        """
        INSERT INTO "CustomerIdentities" (
          "id", "emailHmac", "keyVersion", "personalizationConsentVersion",
          "personalizationConsentedAt", "retentionExpiresAt", "createdAt", "updatedAt"
        ) VALUES (?, ?, 'hmac-v1', 'demand-consent.v1', ?, ?, ?, ?)
        """,
        id,
        "a".repeat(64),
        timestamp(NOW.minus(Duration.ofDays(2))),
        timestamp(NOW.plus(Duration.ofDays(365))),
        timestamp(NOW.minus(Duration.ofDays(2))),
        timestamp(NOW.minus(Duration.ofDays(2))));
  }

  private void insertEvent(UUID eventId, UUID customerId, Instant retention) {
    Instant occurred = NOW.minus(Duration.ofDays(2));
    Instant received = NOW.minus(Duration.ofDays(1));
    jdbc.update(
        """
        INSERT INTO "BehaviorEvents" (
          "eventId", "schemaVersion", "eventType", "eventFamily", "producer", "purpose",
          "consentVersion", "occurredAt", "receivedAt", "requestId", "customerIdentityId",
          "contextJson", "retentionExpiresAt", "createdAt"
        ) VALUES (?, 1, 'searchPerformed', 'discovery', 'web', 'analytics', ?, ?, ?, ?, ?,
          '{}'::jsonb, ?, ?)
        """,
        eventId,
        "demand-consent.v1",
        timestamp(occurred),
        timestamp(received),
        UUID.randomUUID(),
        customerId,
        timestamp(retention),
        timestamp(received));
  }

  private int count(String table, String column, UUID value) {
    Integer result =
        jdbc.queryForObject(
            "SELECT count(*) FROM \"" + table + "\" WHERE \"" + column + "\" = ?",
            Integer.class,
            value);
    return result == null ? 0 : result;
  }

  private static Timestamp timestamp(Instant value) {
    return Timestamp.from(value);
  }
}
