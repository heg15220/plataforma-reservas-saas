package com.reserly.platform.demand.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyPostgreSqlContainerProvider;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Prueba física de recuperación y de todos los filtros duros exigidos por la tarea 20.6. */
@Testcontainers
class HybridCandidateIntegrationTests {
  private static final Instant NOW = Instant.parse("2026-08-14T12:00:00Z");
  private static final LocalDate DATE = LocalDate.of(2026, 8, 15);
  private static final UUID HAIR_CATEGORY = UUID.fromString("20000000-0000-0000-0000-000000000002");
  private static final UUID BEAUTY_CATEGORY =
      UUID.fromString("20000000-0000-0000-0000-000000000007");

  @Container
  private static final PostgreSQLContainer<?> POSTGRESQL =
      (PostgreSQLContainer<?>) new ReserlyPostgreSqlContainerProvider().newInstance();

  private static JdbcTemplate jdbc;
  private static HybridCandidateService service;

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
    service =
        new HybridCandidateServiceImpl(
            jdbc, new HybridCandidateProperties(false, "multilingual-e5-small-v1"));
  }

  @Test
  void returnsOnlyPublishedNearbyCategoryServiceWithRealCapacity() {
    Fixture eligible = fixture("corte-central", HAIR_CATEGORY, "published", -8.5448, 42.8782, true);
    fixture("corte-borrador", HAIR_CATEGORY, "draft", -8.5448, 42.8782, true);
    fixture("corte-madrid", HAIR_CATEGORY, "published", -3.7038, 40.4168, true);
    fixture("corte-estetica", BEAUTY_CATEGORY, "published", -8.5448, 42.8782, true);
    fixture("corte-sin-hueco", HAIR_CATEGORY, "published", -8.55, 42.88, false);

    HybridCandidateQuery query =
        new HybridCandidateQuery(
            "corte",
            "es",
            "peluqueria",
            42.8782,
            -8.5448,
            25_000,
            DATE,
            eligible.serviceId(),
            1,
            10,
            Collections.nCopies(384, 0.0),
            NOW);

    List<HybridCandidate> candidates = service.generate(query);
    assertThat(candidates).hasSize(1);
    assertThat(candidates.getFirst().venueId()).isEqualTo(eligible.venueId());
    assertThat(candidates.getFirst().serviceId()).isEqualTo(eligible.serviceId());
    assertThat(candidates.getFirst().availableSlotCount()).isEqualTo(1);
    assertThat(candidates.getFirst().vectorScore()).isZero();
    assertThat(candidates.getFirst().retrievalPolicyVersion())
        .isEqualTo(HybridCandidateServiceImpl.POLICY_TEXT_ONLY);
  }

  @Test
  void excludesSlotBlockedAfterInitialGeneration() {
    Fixture fixture = fixture("corte-bloqueable", HAIR_CATEGORY, "published", -8.54, 42.87, true);
    HybridCandidateQuery query =
        new HybridCandidateQuery(
            "corte",
            "es",
            "peluqueria",
            42.87,
            -8.54,
            1000,
            DATE,
            fixture.serviceId(),
            1,
            10,
            null,
            NOW);
    assertThat(service.generate(query)).hasSize(1);

    jdbc.update(
        "UPDATE \"TimeSlots\" SET \"status\" = 'blocked' WHERE \"serviceId\" = ?",
        fixture.serviceId());
    assertThat(service.generate(query)).isEmpty();
  }

  @Test
  void usesVersionedValidVectorOnlyWhenFeatureGateIsEnabled() {
    Fixture fixture = fixture("vector-corte", HAIR_CATEGORY, "published", -8.54, 42.87, true);
    List<Double> vector = new java.util.ArrayList<>(Collections.nCopies(384, 0.0));
    vector.set(0, 1.0);
    jdbc.update(
        """
        INSERT INTO "SubjectEmbeddings" (
          "subjectType", "subjectId", "locale", "modelVersion", "dimensions",
          "contentChecksum", "embedding", "validFrom"
        ) VALUES ('venue', ?, 'es', 'multilingual-e5-small-v1', 384, ?, CAST(? AS vector), ?)
        """,
        fixture.venueId(),
        "e".repeat(64),
        vectorLiteral(vector),
        timestamp());
    HybridCandidateService enabled =
        new HybridCandidateServiceImpl(
            jdbc, new HybridCandidateProperties(true, "multilingual-e5-small-v1"));
    HybridCandidateQuery query =
        new HybridCandidateQuery(
            "corte",
            "es",
            "peluqueria",
            42.87,
            -8.54,
            1000,
            DATE,
            fixture.serviceId(),
            1,
            10,
            vector,
            NOW);

    assertThat(enabled.generate(query))
        .singleElement()
        .satisfies(
            candidate -> {
              assertThat(candidate.vectorScore()).isEqualTo(1.0);
              assertThat(candidate.retrievalPolicyVersion())
                  .isEqualTo(HybridCandidateServiceImpl.POLICY_WITH_VECTOR);
            });
  }

  private Fixture fixture(
      String slug,
      UUID categoryId,
      String status,
      double longitude,
      double latitude,
      boolean available) {
    UUID userId = UUID.randomUUID();
    UUID businessId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();
    UUID serviceId = UUID.randomUUID();
    jdbc.update(
        """
        INSERT INTO "Users" (
          "id", "email", "emailNormalized", "passwordHash", "preferredLocale", "status",
          "accountType", "createdAt", "updatedAt"
        ) VALUES (?, ?, ?, 'hash', 'es', 'active', 'venue_business', ?, ?)
        """,
        userId,
        slug + "@example.invalid",
        slug + "@example.invalid",
        timestamp(),
        timestamp());
    jdbc.update(
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
        timestamp(),
        timestamp());
    jdbc.update(
        """
        INSERT INTO "Venues" (
          "id", "ownerUserId", "businessAccountId", "categoryId", "name", "slug",
          "description", "defaultLocale", "status", "manualAvailabilityStatus", "publishedAt",
          "latitude", "longitude", "createdAt", "updatedAt"
        ) VALUES (?, ?, ?, ?, ?, ?, 'corte profesional', 'es', ?, 'automatic', ?, ?, ?, ?, ?)
        """,
        venueId,
        userId,
        businessId,
        categoryId,
        "Corte " + slug,
        slug,
        status,
        "published".equals(status) ? timestamp() : null,
        latitude,
        longitude,
        timestamp(),
        timestamp());
    jdbc.update(
        """
        INSERT INTO "Services" (
          "id", "venueId", "name", "description", "durationMinutes", "capacityRequired",
          "isActive", "createdAt", "updatedAt"
        ) VALUES (?, ?, 'Corte de pelo', 'corte y peinado', 45, 1, true, ?, ?)
        """,
        serviceId,
        venueId,
        timestamp(),
        timestamp());
    jdbc.update(
        """
        INSERT INTO "TimeSlots" (
          "venueId", "serviceId", "date", "weekday", "startsAt", "endsAt", "capacity",
          "status", "createdAt", "updatedAt"
        ) VALUES (?, ?, ?, 6, ?, ?, 1, ?, ?, ?)
        """,
        venueId,
        serviceId,
        Date.valueOf(DATE),
        Time.valueOf(LocalTime.of(10, 0)),
        Time.valueOf(LocalTime.of(10, 45)),
        available ? "available" : "full",
        timestamp(),
        timestamp());
    return new Fixture(venueId, serviceId);
  }

  private Timestamp timestamp() {
    return Timestamp.from(NOW.minusSeconds(60));
  }

  private String vectorLiteral(List<Double> values) {
    return "["
        + values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
        + "]";
  }

  private record Fixture(UUID venueId, UUID serviceId) {}
}
