package com.reserly.platform.demand.recommendation.persistence;

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

/** Prueba física del conjunto candidato y ranking reproducible creado por Flyway V47. */
@Testcontainers
class RecommendationPersistenceIntegrationTests {

  private static final Instant REQUESTED_AT = Instant.parse("2026-08-13T11:00:00Z");

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
  void createsAuditTablesAndPersistsCandidateSetAndRanking() {
    UUID firstVenue = insertVenue("ranking-first");
    UUID secondVenue = insertVenue("ranking-second");
    UUID requestPk = insertRequest(UUID.randomUUID(), "rules", null, "{}", null, null);
    UUID firstCandidate =
        insertCandidate(requestPk, firstVenue, 1, "eligible", true, "ELIGIBLE", "{}");
    insertCandidate(requestPk, secondVenue, 2, "ineligible", false, "NO_CAPACITY", "{}");
    insertRanking(requestPk, firstCandidate, 1, "{\"availability\":0.8}");

    List<String> tables =
        jdbcTemplate.queryForList(
            """
            SELECT "table_name"
            FROM "information_schema"."tables"
            WHERE "table_schema" = current_schema()
              AND "table_name" LIKE 'Recommendation%'
            ORDER BY "table_name"
            """,
            String.class);
    assertThat(tables)
        .containsExactly(
            "RecommendationCandidates", "RecommendationRankings", "RecommendationRequests");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"RecommendationCandidates\" WHERE \"recommendationRequestId\" = ?",
                Long.class,
                requestPk))
        .isEqualTo(2L);
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT \"finalPosition\" FROM \"RecommendationRankings\" WHERE \"recommendationRequestId\" = ?",
                Integer.class,
                requestPk))
        .isEqualTo(1);
  }

  @Test
  void rejectsDuplicateRequestInvalidVisibilityAndUnknownSignals() {
    UUID requestId = UUID.randomUUID();
    insertRequest(requestId, "fallback", null, "{}", null, null);
    assertThatThrownBy(() -> insertRequest(requestId, "fallback", null, "{}", null, null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("uqRecommendationRequestsRequestId");

    UUID requestPk = insertRequest(UUID.randomUUID(), "rules", null, "{}", null, null);
    UUID venue = insertVenue("ranking-invalid");
    assertThatThrownBy(
            () -> insertCandidate(requestPk, venue, 1, "ineligible", true, "NO_CAPACITY", "{}"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckRecommendationCandidatesEligibility");
    assertThatThrownBy(
            () ->
                insertCandidate(
                    requestPk,
                    venue,
                    1,
                    "eligible",
                    false,
                    "ELIGIBLE",
                    "{\"email\":\"forbidden@example.invalid\"}"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckRecommendationCandidatesVisibleSignals");
  }

  @Test
  void rejectsIncompleteVersionsExperimentsAndCrossRequestRanking() {
    assertThatThrownBy(() -> insertRequest(UUID.randomUUID(), "model", null, "{}", null, null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckRecommendationRequestsVersions");
    assertThatThrownBy(() -> insertRequest(UUID.randomUUID(), "rules", null, "{}", "pilot", null))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckRecommendationRequestsExperiment");

    UUID firstRequest = insertRequest(UUID.randomUUID(), "rules", null, "{}", null, null);
    UUID secondRequest = insertRequest(UUID.randomUUID(), "rules", null, "{}", null, null);
    UUID candidate =
        insertCandidate(
            firstRequest, insertVenue("ranking-cross"), 1, "eligible", false, "ELIGIBLE", "{}");
    assertThatThrownBy(() -> insertRanking(secondRequest, candidate, 1, "{}"))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fkRecommendationRankingsCandidateRequest");
  }

  private UUID insertRequest(
      UUID requestId,
      String strategy,
      String modelVersion,
      String context,
      String experimentKey,
      String variantKey) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "RecommendationRequests" (
          "requestId", "schemaVersion", "purpose", "strategy", "policyVersion",
          "modelVersion", "experimentKey", "variantKey", "contextJson", "requestedAt",
          "completedAt", "retentionExpiresAt", "createdAt"
        ) VALUES (?, 1, 'analytics', ?, 'baseline-v1', ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
        RETURNING "id"
        """,
        UUID.class,
        requestId,
        strategy,
        modelVersion,
        experimentKey,
        variantKey,
        context,
        timestamp(REQUESTED_AT),
        timestamp(REQUESTED_AT.plusMillis(25)),
        timestamp(REQUESTED_AT.plus(90, ChronoUnit.DAYS)),
        timestamp(REQUESTED_AT.plusMillis(25)));
  }

  private UUID insertCandidate(
      UUID requestId,
      UUID venueId,
      int position,
      String status,
      boolean visible,
      String reason,
      String signals) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO "RecommendationCandidates" (
          "recommendationRequestId", "venueId", "sourcePosition", "eligibilityStatus",
          "eligibilityReasonCode", "wasVisible", "observedAvailability", "visibleSignalsJson",
          "createdAt"
        ) VALUES (?, ?, ?, ?, ?, ?, true, ?::jsonb, ?)
        RETURNING "id"
        """,
        UUID.class,
        requestId,
        venueId,
        position,
        status,
        reason,
        visible,
        signals,
        timestamp(REQUESTED_AT.plusMillis(10)));
  }

  private void insertRanking(UUID requestId, UUID candidateId, int position, String components) {
    jdbcTemplate.update(
        """
        INSERT INTO "RecommendationRankings" (
          "recommendationRequestId", "recommendationCandidateId", "finalPosition", "score",
          "scoreComponentsJson", "explanationCode", "policyVersion", "rankedAt", "createdAt"
        ) VALUES (?, ?, ?, 0.8, ?::jsonb, 'AVAILABLE_SOON', 'baseline-v1', ?, ?)
        """,
        requestId,
        candidateId,
        position,
        components,
        timestamp(REQUESTED_AT.plusMillis(20)),
        timestamp(REQUESTED_AT.plusMillis(25)));
  }

  private UUID insertVenue(String slug) {
    UUID userId = UUID.randomUUID();
    UUID businessId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();
    UUID categoryId =
        jdbcTemplate.queryForObject(
            "SELECT \"id\" FROM \"Categories\" ORDER BY \"slug\" LIMIT 1", UUID.class);
    jdbcTemplate.update(
        """
        INSERT INTO "Users" (
          "id", "email", "emailNormalized", "passwordHash", "preferredLocale", "status",
          "accountType", "createdAt", "updatedAt"
        ) VALUES (?, ?, ?, 'hash', 'es', 'active', 'venue_business', ?, ?)
        """,
        userId,
        slug + "@example.invalid",
        slug + "@example.invalid",
        timestamp(REQUESTED_AT.minusSeconds(60)),
        timestamp(REQUESTED_AT.minusSeconds(60)));
    jdbcTemplate.update(
        """
        INSERT INTO "BusinessAccounts" (
          "id", "ownerUserId", "taxCountry", "businessLegalName", "businessTaxIdentifier",
          "businessTaxIdentifierNormalized", "createdAt", "updatedAt"
        ) VALUES (?, ?, 'ES', ?, ?, ?, ?, ?)
        """,
        businessId,
        userId,
        slug,
        "TAX-" + slug,
        "TAX-" + slug,
        timestamp(REQUESTED_AT.minusSeconds(60)),
        timestamp(REQUESTED_AT.minusSeconds(60)));
    jdbcTemplate.update(
        """
        INSERT INTO "Venues" (
          "id", "ownerUserId", "businessAccountId", "categoryId", "name", "slug",
          "defaultLocale", "status", "manualAvailabilityStatus", "createdAt", "updatedAt"
        ) VALUES (?, ?, ?, ?, ?, ?, 'es', 'draft', 'automatic', ?, ?)
        """,
        venueId,
        userId,
        businessId,
        categoryId,
        slug,
        slug,
        timestamp(REQUESTED_AT.minusSeconds(30)),
        timestamp(REQUESTED_AT.minusSeconds(30)));
    return venueId;
  }

  private static Timestamp timestamp(Instant instant) {
    return Timestamp.from(instant);
  }
}
