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

  @Test
  void supportsAccessCorrectionPurposeRevocationAndUnlinkWithoutExposingHmac() {
    UUID customerId = UUID.randomUUID();
    UUID anonymousId = insertAnonymous();
    insertCustomer(customerId);
    insertLink(anonymousId, customerId, "personalization");
    insertLink(anonymousId, customerId, "analytics");
    DemandPrivacyService service = service();

    DemandPrivacyResponse access = service.execute(request(customerId, "access", null, null, null));
    assertThat(access.result())
        .containsEntry("identityFound", true)
        .containsEntry("profiles", 0)
        .containsEntry("links", 2);

    String replacementHmac = "b".repeat(64);
    DemandPrivacyResponse correction =
        service.execute(request(customerId, "correction", null, replacementHmac, "hmac-v2"));
    assertThat(correction.result()).containsEntry("corrected", true);
    assertThat(
            jdbc.queryForObject(
                "SELECT \"emailHmac\" FROM \"CustomerIdentities\" WHERE \"id\" = ?",
                String.class,
                customerId))
        .isEqualTo(replacementHmac);

    DemandPrivacyResponse revocation =
        service.execute(request(customerId, "revocation", "personalization", null, null));
    assertThat(revocation.result())
        .containsEntry("linksRevoked", 1)
        .containsEntry("consentRevoked", true);
    DemandPrivacyResponse unlink = service.execute(request(customerId, "unlink", null, null, null));
    assertThat(unlink.result()).containsEntry("linksRevoked", 1);
    assertThat(
            jdbc.queryForObject(
                "SELECT bool_and(\"revokedAt\" IS NOT NULL) FROM \"IdentityLinks\" "
                    + "WHERE \"customerIdentityId\" = ?",
                Boolean.class,
                customerId))
        .isTrue();
    assertThat(
            jdbc.queryForObject(
                "SELECT string_agg(\"resultJson\"::text, '') FROM \"DemandPrivacyRequests\" "
                    + "WHERE \"subjectId\" = ?",
                String.class,
                customerId))
        .doesNotContain(replacementHmac);
  }

  @Test
  void oppositionRevokesEveryActivePurpose() {
    UUID customerId = UUID.randomUUID();
    UUID anonymousId = insertAnonymous();
    insertCustomer(customerId);
    insertLink(anonymousId, customerId, "personalization");
    insertLink(anonymousId, customerId, "analytics");

    DemandPrivacyResponse opposition =
        service().execute(request(customerId, "objection", null, null, null));

    assertThat(opposition.result())
        .containsEntry("linksRevoked", 2)
        .containsEntry("consentRevoked", true);
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
        id.toString().replace("-", "").repeat(2),
        timestamp(NOW.minus(Duration.ofDays(2))),
        timestamp(NOW.plus(Duration.ofDays(365))),
        timestamp(NOW.minus(Duration.ofDays(2))),
        timestamp(NOW.minus(Duration.ofDays(2))));
  }

  private UUID insertAnonymous() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO "AnonymousIdentities" (
          "id", "channel", "personalizationConsentVersion", "personalizationConsentedAt",
          "createdAt", "lastSeenAt", "expiresAt", "retentionExpiresAt"
        ) VALUES (?, 'browser', 'demand-consent.v1', ?, ?, ?, ?, ?)
        """,
        id,
        timestamp(NOW.minus(Duration.ofDays(2))),
        timestamp(NOW.minus(Duration.ofDays(2))),
        timestamp(NOW.minus(Duration.ofDays(1))),
        timestamp(NOW.plus(Duration.ofDays(30))),
        timestamp(NOW.plus(Duration.ofDays(90))));
    return id;
  }

  private void insertLink(UUID anonymousId, UUID customerId, String purpose) {
    jdbc.update(
        """
        INSERT INTO "IdentityLinks" (
          "anonymousIdentityId", "customerIdentityId", "linkReason", "purpose",
          "consentVersion", "consentedAt", "linkedAt", "retentionExpiresAt", "createdAt"
        ) VALUES (?, ?, 'booking_email_confirmed', ?, 'demand-consent.v1', ?, ?, ?, ?)
        """,
        anonymousId,
        customerId,
        purpose,
        timestamp(NOW.minus(Duration.ofDays(2))),
        timestamp(NOW.minus(Duration.ofDays(1))),
        timestamp(NOW.plus(Duration.ofDays(90))),
        timestamp(NOW.minus(Duration.ofDays(1))));
  }

  private DemandPrivacyService service() {
    return new DemandPrivacyService(jdbc, new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private DemandPrivacyRequest request(
      UUID subjectId, String action, String purpose, String hmac, String keyVersion) {
    return new DemandPrivacyRequest(
        UUID.randomUUID(), subjectId, "customer", action, purpose, hmac, keyVersion);
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
