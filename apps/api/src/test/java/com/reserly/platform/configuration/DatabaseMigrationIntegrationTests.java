package com.reserly.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifica una migración completa sobre una instancia PostGIS efímera y vacía.
 *
 * <p>El perfil {@code test} usa Testcontainers JDBC. Flyway debe crear su historial y activar las
 * extensiones antes de que Hibernate valide el esquema.
 */
@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationIntegrationTests {

  @Autowired private Flyway flyway;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void migratesEmptyPostgisDatabaseToLatestVersion() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("9");

    List<String> extensions =
        jdbcTemplate.queryForList(
            """
                        SELECT "extname"
                        FROM "pg_extension"
                        WHERE "extname" IN ('postgis', 'pg_trgm', 'unaccent')
                        ORDER BY "extname"
                        """,
            String.class);

    assertThat(extensions).containsExactly("pg_trgm", "postgis", "unaccent");
  }

  @Test
  void usesUtf8AndUtcForDatabaseSessions() {
    String encoding = jdbcTemplate.queryForObject("SHOW server_encoding", String.class);
    String timezone = jdbcTemplate.queryForObject("SHOW TIMEZONE", String.class);

    assertThat(encoding).isEqualToIgnoringCase("UTF8");
    assertThat(timezone).isEqualToIgnoringCase("UTC");
  }

  /**
   * Protege el contrato físico de la Fase 2 frente a renombres accidentales o migraciones
   * incompletas. Las colecciones preservan el orden ordinal de PostgreSQL para que el test también
   * detecte columnas omitidas o añadidas sin actualizar conscientemente este contrato.
   */
  @Test
  void createsVenueCatalogTablesWithExpectedColumns() {
    Map<String, List<String>> expectedColumns = new LinkedHashMap<>();
    expectedColumns.put(
        "Categories",
        List.of(
            "id",
            "name",
            "nameI18n",
            "slug",
            "description",
            "descriptionI18n",
            "isActive",
            "createdAt",
            "updatedAt"));
    expectedColumns.put(
        "Venues",
        List.of(
            "id",
            "ownerUserId",
            "businessAccountId",
            "categoryId",
            "name",
            "slug",
            "description",
            "descriptionI18n",
            "defaultLocale",
            "contactEmail",
            "phone",
            "address",
            "city",
            "province",
            "country",
            "postalCode",
            "latitude",
            "longitude",
            "location",
            "mainImageUrl",
            "status",
            "manualAvailabilityStatus",
            "showPhone",
            "showEmail",
            "publishedAt",
            "createdAt",
            "updatedAt"));
    expectedColumns.put(
        "VenueImages", List.of("id", "venueId", "url", "altText", "position", "createdAt"));

    expectedColumns.forEach(
        (table, columns) ->
            assertThat(
                    jdbcTemplate.queryForList(
                        """
                        SELECT "column_name"
                        FROM "information_schema"."columns"
                        WHERE "table_schema" = current_schema()
                          AND "table_name" = ?
                        ORDER BY "ordinal_position"
                        """,
                        String.class,
                        table))
                .as("columnas físicas de %s", table)
                .containsExactlyElementsOf(columns));
  }

  @Test
  void createsVenueSearchAndLocationIndexes() {
    List<String> indexes =
        jdbcTemplate.queryForList(
            """
            SELECT "indexname"
            FROM "pg_indexes"
            WHERE "schemaname" = current_schema()
              AND "tablename" = 'Venues'
            ORDER BY "indexname"
            """,
            String.class);

    assertThat(indexes)
        .contains(
            "ixVenuesCategoryStatus",
            "ixVenuesLocation",
            "ixVenuesPublishedNameTrigram",
            "ixVenuesPublicLocation",
            "uqVenuesSlug");
  }

  /**
   * Demuestra sobre PostgreSQL que las restricciones críticas no dependen del futuro CRUD. Se
   * emplean identificadores aleatorios y limpieza explícita porque cada rechazo debe ejecutarse en
   * su propia transacción autocommit: PostgreSQL invalida la transacción que viola una constraint.
   */
  @Test
  void enforcesVenueOwnershipLocalizationCoordinatesAndImageOrder() {
    UUID ownerUserId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    UUID businessAccountId = UUID.randomUUID();
    UUID categoryId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();

    try {
      insertVenueOwner(ownerUserId, "owner-" + ownerUserId + "@example.invalid");
      insertVenueOwner(otherUserId, "other-" + otherUserId + "@example.invalid");
      jdbcTemplate.update(
          """
          INSERT INTO "BusinessAccounts" (
            "id", "ownerUserId", "taxCountry", "businessLegalName",
            "businessTaxIdentifier", "businessTaxIdentifierNormalized"
          ) VALUES (?, ?, 'ES', 'Negocio de prueba', ?, ?)
          """,
          businessAccountId,
          ownerUserId,
          "B" + ownerUserId.toString().substring(0, 8),
          "B" + ownerUserId.toString().substring(0, 8));

      assertThatThrownBy(
              () ->
                  jdbcTemplate.update(
                      """
                      INSERT INTO "Categories" ("name", "nameI18n", "slug")
                      VALUES (
                        'Inválida',
                        '{"sourceLocale":"es","values":{"es":"Inválida"}}'::jsonb,
                        ?
                      )
                      """,
                      "invalid-" + categoryId))
          .isInstanceOf(DataIntegrityViolationException.class);

      jdbcTemplate.update(
          """
          INSERT INTO "Categories" ("id", "name", "nameI18n", "slug")
          VALUES (
            ?,
            'Restaurante',
            '{"sourceLocale":"es","values":{"es":"Restaurante","en":"Restaurant"}}'::jsonb,
            ?
          )
          """,
          categoryId,
          "restaurant-" + categoryId);

      assertThatThrownBy(
              () ->
                  insertVenue(
                      UUID.randomUUID(),
                      otherUserId,
                      businessAccountId,
                      categoryId,
                      "foreign-owner-" + venueId,
                      null,
                      null))
          .isInstanceOf(DataIntegrityViolationException.class);

      assertThatThrownBy(
              () ->
                  insertVenue(
                      UUID.randomUUID(),
                      ownerUserId,
                      businessAccountId,
                      categoryId,
                      "partial-coordinates-" + venueId,
                      40.416775,
                      null))
          .isInstanceOf(DataIntegrityViolationException.class);

      insertVenue(
          venueId,
          ownerUserId,
          businessAccountId,
          categoryId,
          "valid-venue-" + venueId,
          40.416775,
          -3.703790);

      Map<String, Object> point =
          jdbcTemplate.queryForMap(
              """
              SELECT
                ST_Y("location"::geometry) AS "latitude",
                ST_X("location"::geometry) AS "longitude"
              FROM "Venues"
              WHERE "id" = ?
              """,
              venueId);
      assertThat(((Number) point.get("latitude")).doubleValue())
          .isCloseTo(40.416775, withinCoordinateTolerance());
      assertThat(((Number) point.get("longitude")).doubleValue())
          .isCloseTo(-3.703790, withinCoordinateTolerance());

      jdbcTemplate.update(
          """
          INSERT INTO "VenueImages" ("venueId", "url", "altText", "position")
          VALUES (?, 'venues/example/gallery-1.webp', 'Fachada', 0)
          """,
          venueId);

      assertThatThrownBy(
              () ->
                  jdbcTemplate.update(
                      """
                      INSERT INTO "VenueImages" ("venueId", "url", "position")
                      VALUES (?, 'venues/example/gallery-2.webp', 0)
                      """,
                      venueId))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbcTemplate.update(
          "DELETE FROM \"Venues\" WHERE \"businessAccountId\" = ?", businessAccountId);
      jdbcTemplate.update("DELETE FROM \"Categories\" WHERE \"id\" = ?", categoryId);
      jdbcTemplate.update("DELETE FROM \"BusinessAccounts\" WHERE \"id\" = ?", businessAccountId);
      jdbcTemplate.update("DELETE FROM \"Users\" WHERE \"id\" IN (?, ?)", ownerUserId, otherUserId);
    }
  }

  private void insertVenueOwner(UUID userId, String email) {
    jdbcTemplate.update(
        """
        INSERT INTO "Users" (
          "id", "email", "emailNormalized", "passwordHash", "preferredLocale",
          "status", "accountType"
        ) VALUES (?, ?, ?, 'test-password-hash', 'es', 'active', 'venue_business')
        """,
        userId,
        email,
        email);
  }

  private void insertVenue(
      UUID venueId,
      UUID ownerUserId,
      UUID businessAccountId,
      UUID categoryId,
      String slug,
      Double latitude,
      Double longitude) {
    jdbcTemplate.update(
        """
        INSERT INTO "Venues" (
          "id", "ownerUserId", "businessAccountId", "categoryId", "name", "slug",
          "latitude", "longitude"
        ) VALUES (?, ?, ?, ?, 'Local de prueba', ?, ?, ?)
        """,
        venueId,
        ownerUserId,
        businessAccountId,
        categoryId,
        slug,
        latitude,
        longitude);
  }

  private org.assertj.core.data.Offset<Double> withinCoordinateTolerance() {
    return org.assertj.core.data.Offset.offset(0.000001);
  }
}
