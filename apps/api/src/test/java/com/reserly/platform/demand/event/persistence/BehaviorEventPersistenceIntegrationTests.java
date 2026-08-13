package com.reserly.platform.demand.event.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.configuration.ReserlyPostgreSqlContainerProvider;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Prueba física de idempotencia, tiempo, finalidad y contexto minimizado de `BehaviorEvents`. */
@Testcontainers
class BehaviorEventPersistenceIntegrationTests {

  private static final Instant OCCURRED_AT = Instant.parse("2026-08-13T09:00:00Z");
  private static final Instant RECEIVED_AT = Instant.parse("2026-08-13T09:10:00Z");

  @Container
  private static final PostgreSQLContainer<?> POSTGRESQL =
      (PostgreSQLContainer<?>) new ReserlyPostgreSqlContainerProvider().newInstance();

  private static JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void migrateEmptyDatabase() {
    Flyway.configure()
        .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
        .locations("classpath:db/migration")
        .load()
        .migrate();
    jdbcTemplate =
        new JdbcTemplate(
            new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword()));
  }

  @Test
  void persistsLateEventWithSeparateTimesAndExpectedIndexes() {
    UUID eventId = UUID.randomUUID();
    insertDiscoveryEvent(eventId, "{\"queryLength\":12,\"resultCount\":8}");

    var row =
        jdbcTemplate.queryForMap(
            """
            SELECT "occurredAt", "receivedAt", "contextJson"
            FROM "BehaviorEvents"
            WHERE "eventId" = ?
            """,
            eventId);
    assertThat(((Timestamp) row.get("occurredAt")).toInstant()).isEqualTo(OCCURRED_AT);
    assertThat(((Timestamp) row.get("receivedAt")).toInstant()).isEqualTo(RECEIVED_AT);

    List<String> indexes =
        jdbcTemplate.queryForList(
            """
            SELECT "indexname"
            FROM "pg_indexes"
            WHERE "tablename" = 'BehaviorEvents'
            ORDER BY "indexname"
            """,
            String.class);
    assertThat(indexes)
        .contains(
            "uqBehaviorEventsEventId",
            "ixBehaviorEventsOccurredAt",
            "ixBehaviorEventsTypeOccurredAt",
            "ixBehaviorEventsVenueOccurredAt",
            "ixBehaviorEventsRetention");
  }

  @Test
  void rejectsDuplicateEventIdWrongFamilyAndUnknownContextKey() {
    UUID eventId = UUID.randomUUID();
    insertDiscoveryEvent(eventId, "{\"queryLength\":12}");

    assertThatThrownBy(() -> insertDiscoveryEvent(eventId, "{\"queryLength\":12}"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uqBehaviorEventsEventId");

    assertThatThrownBy(
            () ->
                insertEvent(
                    UUID.randomUUID(),
                    "searchPerformed",
                    "conversion",
                    "{\"stepCode\":\"started\"}"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBehaviorEventsTypeFamily");

    assertThatThrownBy(
            () ->
                insertDiscoveryEvent(
                    UUID.randomUUID(), "{\"queryLength\":12,\"email\":\"x@example.invalid\"}"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBehaviorEventsContextKeys");
  }

  @Test
  void rejectsPersistentIdentityWithoutConsentAndInvalidTemporalOrder() {
    UUID anonymousId = insertAnonymousIdentity();

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "BehaviorEvents" (
                      "eventId", "schemaVersion", "eventType", "eventFamily", "producer",
                      "purpose", "occurredAt", "receivedAt", "requestId", "sessionId",
                      "anonymousIdentityId", "contextJson", "retentionExpiresAt", "createdAt"
                    ) VALUES (?, 1, 'searchPerformed', 'discovery', 'web', 'analytics',
                      ?, ?, ?, ?, ?, '{}'::jsonb, ?, ?)
                    """,
                    UUID.randomUUID(),
                    timestamp(OCCURRED_AT),
                    timestamp(RECEIVED_AT),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    anonymousId,
                    timestamp(RECEIVED_AT.plus(90, ChronoUnit.DAYS)),
                    timestamp(RECEIVED_AT)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBehaviorEventsConsent");

    assertThatThrownBy(
            () ->
                insertEventWithTimes(
                    UUID.randomUUID(), RECEIVED_AT, OCCURRED_AT, "{\"queryLength\":1}"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckBehaviorEventsTimes");
  }

  private void insertDiscoveryEvent(UUID eventId, String contextJson) {
    insertEvent(eventId, "searchPerformed", "discovery", contextJson);
  }

  private void insertEvent(UUID eventId, String eventType, String family, String contextJson) {
    jdbcTemplate.update(
        """
        INSERT INTO "BehaviorEvents" (
          "eventId", "schemaVersion", "eventType", "eventFamily", "producer", "purpose",
          "occurredAt", "receivedAt", "requestId", "sessionId", "contextJson",
          "retentionExpiresAt", "createdAt"
        ) VALUES (?, 1, ?, ?, 'web', 'analytics', ?, ?, ?, ?, ?::jsonb, ?, ?)
        """,
        eventId,
        eventType,
        family,
        timestamp(OCCURRED_AT),
        timestamp(RECEIVED_AT),
        UUID.randomUUID(),
        UUID.randomUUID(),
        contextJson,
        timestamp(RECEIVED_AT.plus(90, ChronoUnit.DAYS)),
        timestamp(RECEIVED_AT));
  }

  private void insertEventWithTimes(
      UUID eventId, Instant occurredAt, Instant receivedAt, String contextJson) {
    jdbcTemplate.update(
        """
        INSERT INTO "BehaviorEvents" (
          "eventId", "schemaVersion", "eventType", "eventFamily", "producer", "purpose",
          "occurredAt", "receivedAt", "requestId", "contextJson",
          "retentionExpiresAt", "createdAt"
        ) VALUES (?, 1, 'searchPerformed', 'discovery', 'web', 'analytics',
          ?, ?, ?, ?::jsonb, ?, ?)
        """,
        eventId,
        timestamp(occurredAt),
        timestamp(receivedAt),
        UUID.randomUUID(),
        contextJson,
        timestamp(receivedAt.plus(90, ChronoUnit.DAYS)),
        timestamp(receivedAt));
  }

  private UUID insertAnonymousIdentity() {
    UUID identityId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO "AnonymousIdentities" (
          "id", "channel", "createdAt", "lastSeenAt", "expiresAt", "retentionExpiresAt"
        ) VALUES (?, 'browser', ?, ?, ?, ?)
        """,
        identityId,
        timestamp(OCCURRED_AT.minusSeconds(60)),
        timestamp(OCCURRED_AT),
        timestamp(OCCURRED_AT.plus(30, ChronoUnit.DAYS)),
        timestamp(OCCURRED_AT.plus(90, ChronoUnit.DAYS)));
    return identityId;
  }

  private static Timestamp timestamp(Instant instant) {
    return Timestamp.from(instant);
  }
}
