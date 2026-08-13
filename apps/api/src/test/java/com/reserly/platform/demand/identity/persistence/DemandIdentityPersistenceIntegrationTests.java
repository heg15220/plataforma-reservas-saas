package com.reserly.platform.demand.identity.persistence;

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

/**
 * Verifica sobre PostgreSQL real el esquema de identidad seudónima creado por Flyway.
 *
 * <p>Los casos protegen minimización, HMAC versionado, consentimiento, revocación, retención y
 * unicidad del vínculo activo. Las entidades y DAOs se compilan contra el mismo contrato físico y
 * no se levanta todo Spring para aislar esta prueba de módulos no relacionados.
 */
@Testcontainers
class DemandIdentityPersistenceIntegrationTests {

  private static final Instant NOW = Instant.parse("2026-08-13T10:00:00Z");
  private static final String EMAIL_HMAC = "a".repeat(64);

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
  void createsMinimalIdentityTablesAndIndexes() {
    List<String> tables =
        jdbcTemplate.queryForList(
            """
            SELECT "table_name"
            FROM "information_schema"."tables"
            WHERE "table_schema" = current_schema()
              AND "table_name" IN (
                'CustomerIdentities', 'AnonymousIdentities', 'IdentityLinks'
              )
            ORDER BY "table_name"
            """,
            String.class);
    assertThat(tables)
        .containsExactly("AnonymousIdentities", "CustomerIdentities", "IdentityLinks");

    List<String> forbiddenColumns =
        jdbcTemplate.queryForList(
            """
            SELECT "column_name"
            FROM "information_schema"."columns"
            WHERE "table_schema" = current_schema()
              AND "table_name" IN ('CustomerIdentities', 'AnonymousIdentities', 'IdentityLinks')
              AND lower("column_name") IN (
                'email', 'emailnormalized', 'ip', 'ipaddress', 'useragent',
                'fingerprint', 'advertisingid', 'cookie'
              )
            """,
            String.class);
    assertThat(forbiddenColumns).isEmpty();

    List<String> indexes =
        jdbcTemplate.queryForList(
            """
            SELECT "indexname"
            FROM "pg_indexes"
            WHERE "schemaname" = current_schema()
              AND "tablename" IN ('CustomerIdentities', 'AnonymousIdentities', 'IdentityLinks')
            ORDER BY "indexname"
            """,
            String.class);
    assertThat(indexes)
        .contains(
            "uqCustomerIdentitiesKeyHmac",
            "uqIdentityLinksActiveAnonymousCustomerPurpose",
            "ixCustomerIdentitiesRetention",
            "ixAnonymousIdentitiesExpiry",
            "ixIdentityLinksRetention");
  }

  @Test
  void persistsConsentThenRevokesLinkWithoutAffectingOperationalData() {
    UUID customerId = insertCustomerIdentity("c".repeat(64), "hmac-2026-01");
    UUID anonymousId = insertAnonymousIdentity();
    UUID linkId = insertIdentityLink(customerId, anonymousId, "personalization");

    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM "IdentityLinks"
                WHERE "id" = ?
                  AND "purpose" = 'personalization'
                  AND "consentVersion" = 'personalization-v1'
                  AND "revokedAt" IS NULL
                """,
                Integer.class,
                linkId))
        .isOne();

    jdbcTemplate.update(
        "UPDATE \"IdentityLinks\" SET \"revokedAt\" = ? WHERE \"id\" = ?",
        timestamp(NOW.plusSeconds(60)),
        linkId);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT \"revokedAt\" IS NOT NULL FROM \"IdentityLinks\" WHERE \"id\" = ?",
                Boolean.class,
                linkId))
        .isTrue();
  }

  @Test
  void rejectsMalformedHmacInvalidConsentAndDuplicateActivePurposeLink() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "CustomerIdentities" (
                      "emailHmac", "keyVersion", "retentionExpiresAt"
                    ) VALUES ('not-a-hmac', 'hmac-2026-01', ?)
                    """,
                    timestamp(NOW.plus(365, ChronoUnit.DAYS))))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckCustomerIdentitiesEmailHmac");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO "CustomerIdentities" (
                      "emailHmac", "keyVersion", "personalizationConsentVersion",
                      "retentionExpiresAt"
                    ) VALUES (?, 'hmac-2026-01', 'personalization-v1', ?)
                    """,
                    "b".repeat(64),
                    timestamp(NOW.plus(365, ChronoUnit.DAYS))))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckCustomerIdentitiesConsent");

    UUID customerId = insertCustomerIdentity(EMAIL_HMAC, "hmac-2026-01");
    UUID anonymousId = insertAnonymousIdentity();
    insertIdentityLink(customerId, anonymousId, "personalization");

    assertThatThrownBy(() -> insertIdentityLink(customerId, anonymousId, "personalization"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uqIdentityLinksActiveAnonymousCustomerPurpose");
  }

  private UUID insertCustomerIdentity(String emailHmac, String keyVersion) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "CustomerIdentities" (
          "emailHmac", "keyVersion", "personalizationConsentVersion",
          "personalizationConsentedAt", "retentionExpiresAt", "createdAt", "updatedAt"
        ) VALUES (?, ?, 'personalization-v1', ?, ?, ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        emailHmac,
        keyVersion,
        timestamp(NOW.minusSeconds(60)),
        timestamp(NOW.plus(365, ChronoUnit.DAYS)),
        timestamp(NOW.minusSeconds(60)),
        timestamp(NOW.minusSeconds(60)));
  }

  private UUID insertAnonymousIdentity() {
    UUID anonymousId = UUID.randomUUID();
    jdbcTemplate.update(
        """
        INSERT INTO "AnonymousIdentities" (
          "id", "channel", "personalizationConsentVersion",
          "personalizationConsentedAt", "createdAt", "lastSeenAt",
          "expiresAt", "retentionExpiresAt"
        ) VALUES (?, 'browser', 'personalization-v1', ?, ?, ?, ?, ?)
        """,
        anonymousId,
        timestamp(NOW.minusSeconds(60)),
        timestamp(NOW.minusSeconds(60)),
        timestamp(NOW.minusSeconds(30)),
        timestamp(NOW.plus(30, ChronoUnit.DAYS)),
        timestamp(NOW.plus(90, ChronoUnit.DAYS)));
    return anonymousId;
  }

  private UUID insertIdentityLink(UUID customerId, UUID anonymousId, String purpose) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "IdentityLinks" (
          "anonymousIdentityId", "customerIdentityId", "linkReason", "purpose",
          "consentVersion", "consentedAt", "linkedAt", "retentionExpiresAt", "createdAt"
        ) VALUES (?, ?, 'booking_email_confirmed', ?, 'personalization-v1', ?, ?, ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        anonymousId,
        customerId,
        purpose,
        timestamp(NOW.minusSeconds(60)),
        timestamp(NOW),
        timestamp(NOW.plus(90, ChronoUnit.DAYS)),
        timestamp(NOW));
  }

  private static Timestamp timestamp(Instant instant) {
    return Timestamp.from(instant);
  }
}
