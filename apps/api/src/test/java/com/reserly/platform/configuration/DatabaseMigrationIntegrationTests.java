package com.reserly.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("12");

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
   * Verifica la identidad estable y el contenido mínimo de la taxonomía inicial. Se filtra por el
   * prefijo UUID reservado para no impedir que futuras migraciones añadan categorías adicionales.
   */
  @Test
  void seedsInitialVenueCategories() {
    List<Map<String, Object>> categories =
        jdbcTemplate.queryForList(
            """
            SELECT
              "id"::text AS "id",
              "name",
              "slug",
              "nameI18n"->>'sourceLocale' AS "sourceLocale",
              "nameI18n"->'values'->>'es' AS "nameEs",
              "nameI18n"->'values'->>'en' AS "nameEn",
              "isActive"
            FROM "Categories"
            WHERE "id"::text LIKE '20000000-0000-0000-0000-00000000000_'
            ORDER BY "id"
            """);

    assertThat(categories)
        .containsExactly(
            categorySeedRow(
                "20000000-0000-0000-0000-000000000001", "Restaurante", "restaurante", "Restaurant"),
            categorySeedRow(
                "20000000-0000-0000-0000-000000000002", "Peluquería", "peluqueria", "Hair salon"),
            categorySeedRow(
                "20000000-0000-0000-0000-000000000003",
                "Campo de fútbol",
                "campo-de-futbol",
                "Football pitch"),
            categorySeedRow(
                "20000000-0000-0000-0000-000000000004",
                "Pista de pádel",
                "pista-de-padel",
                "Padel court"),
            categorySeedRow(
                "20000000-0000-0000-0000-000000000005",
                "Instalación municipal",
                "instalacion-municipal",
                "Municipal facility"),
            categorySeedRow(
                "20000000-0000-0000-0000-000000000006",
                "Centro deportivo",
                "centro-deportivo",
                "Sports center"),
            categorySeedRow(
                "20000000-0000-0000-0000-000000000007",
                "Centro de estética",
                "centro-de-estetica",
                "Beauty center"),
            categorySeedRow("20000000-0000-0000-0000-000000000008", "Otros", "otros", "Other"));
  }

  /**
   * Audita el contenido bilingüe persistido y lo atraviesa por el mismo value object que usarán los
   * futuros servicios. La resolución con locale nulo demuestra además el fallback inglés sin
   * exponer el documento JSONB al consumidor.
   */
  @Test
  void resolvesCompleteInitialCategoryTranslations() {
    Map<String, CategoryTranslationExpectation> expected = initialCategoryTranslations();
    Map<String, CategoryTranslationExpectation> persisted = new LinkedHashMap<>();

    jdbcTemplate.query(
        """
        SELECT
          "slug",
          "nameI18n"->>'sourceLocale' AS "sourceLocale",
          "nameI18n"->'values'->>'es' AS "nameEs",
          "nameI18n"->'values'->>'en' AS "nameEn",
          "descriptionI18n"->'values'->>'es' AS "descriptionEs",
          "descriptionI18n"->'values'->>'en' AS "descriptionEn"
        FROM "Categories"
        WHERE "id"::text LIKE '20000000-0000-0000-0000-00000000000_'
        ORDER BY "id"
        """,
        resultSet -> {
          String sourceLocale = resultSet.getString("sourceLocale");
          CategoryTranslationExpectation translation =
              new CategoryTranslationExpectation(
                  resultSet.getString("nameEs"),
                  resultSet.getString("nameEn"),
                  resultSet.getString("descriptionEs"),
                  resultSet.getString("descriptionEn"));
          persisted.put(resultSet.getString("slug"), translation);

          LocalizedText name =
              LocalizedText.fromLanguageTagValues(
                  sourceLocale, Map.of("es", translation.nameEs(), "en", translation.nameEn()));
          LocalizedText description =
              LocalizedText.fromLanguageTagValues(
                  sourceLocale,
                  Map.of("es", translation.descriptionEs(), "en", translation.descriptionEn()));

          assertThat(name.hasRequiredTranslations(Set.of(SupportedLocale.ES, SupportedLocale.EN)))
              .isTrue();
          assertThat(description.resolve(SupportedLocale.ES)).contains(translation.descriptionEs());
          assertThat(description.resolve(SupportedLocale.EN)).contains(translation.descriptionEn());
          assertThat(description.resolve(null)).contains(translation.descriptionEn());
        });

    assertThat(persisted).containsExactlyEntriesOf(expected);

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    UPDATE "Categories"
                    SET "descriptionI18n" =
                      '{"sourceLocale":"es","values":{"es":"Solo español"}}'::jsonb
                    WHERE "id" = '20000000-0000-0000-0000-000000000008'
                    """))
        .isInstanceOf(DataIntegrityViolationException.class);
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
            "uqVenuesOwnerCurrent",
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

  private Map<String, Object> categorySeedRow(
      String id, String spanishName, String slug, String englishName) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", id);
    row.put("name", spanishName);
    row.put("slug", slug);
    row.put("sourceLocale", "es");
    row.put("nameEs", spanishName);
    row.put("nameEn", englishName);
    row.put("isActive", true);
    return row;
  }

  private Map<String, CategoryTranslationExpectation> initialCategoryTranslations() {
    Map<String, CategoryTranslationExpectation> translations = new LinkedHashMap<>();
    translations.put(
        "restaurante",
        new CategoryTranslationExpectation(
            "Restaurante",
            "Restaurant",
            "Restaurantes y espacios gastronómicos con reserva de mesa.",
            "Restaurants and dining venues with table reservations."));
    translations.put(
        "peluqueria",
        new CategoryTranslationExpectation(
            "Peluquería",
            "Hair salon",
            "Peluquerías y salones para servicios de cuidado del cabello.",
            "Hairdressers and salons offering hair care services."));
    translations.put(
        "campo-de-futbol",
        new CategoryTranslationExpectation(
            "Campo de fútbol",
            "Football pitch",
            "Campos e instalaciones para reservar partidos y entrenamientos de fútbol.",
            "Football pitches and facilities for booking matches and training sessions."));
    translations.put(
        "pista-de-padel",
        new CategoryTranslationExpectation(
            "Pista de pádel",
            "Padel court",
            "Pistas e instalaciones para reservar partidos y entrenamientos de pádel.",
            "Padel courts and facilities for booking matches and training sessions."));
    translations.put(
        "instalacion-municipal",
        new CategoryTranslationExpectation(
            "Instalación municipal",
            "Municipal facility",
            "Espacios y servicios municipales disponibles mediante reserva.",
            "Municipal spaces and services available by reservation."));
    translations.put(
        "centro-deportivo",
        new CategoryTranslationExpectation(
            "Centro deportivo",
            "Sports center",
            "Centros con actividades, clases e instalaciones deportivas reservables.",
            "Centers with bookable sports activities, classes and facilities."));
    translations.put(
        "centro-de-estetica",
        new CategoryTranslationExpectation(
            "Centro de estética",
            "Beauty center",
            "Centros para reservar tratamientos de estética y cuidado personal.",
            "Centers for booking beauty and personal care treatments."));
    translations.put(
        "otros",
        new CategoryTranslationExpectation(
            "Otros",
            "Other",
            "Otros negocios, servicios y espacios que funcionan con reserva.",
            "Other businesses, services and spaces that operate by reservation."));
    return translations;
  }

  /** Contenido visible esperado por locale para una categoría inicial. */
  private record CategoryTranslationExpectation(
      String nameEs, String nameEn, String descriptionEs, String descriptionEn) {}
}
