package com.reserly.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
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
 * Contrato aislado de la migración pgvector contra la imagen PostgreSQL real de Reserly.
 *
 * <p>No levanta el contexto Spring: aplica todo Flyway directamente y permite diagnosticar
 * compatibilidad de imagen/esquema aunque otro módulo de la aplicación tenga un fallo de arranque.
 */
@Testcontainers
class PgvectorMigrationIntegrationTests {

  @Container
  private static final PostgreSQLContainer<?> POSTGRESQL =
      (PostgreSQLContainer<?>) new ReserlyPostgreSqlContainerProvider().newInstance();

  private static JdbcTemplate jdbcTemplate;
  private static Flyway flyway;

  @BeforeAll
  static void migrateEmptyDatabase() {
    flyway =
        Flyway.configure()
            .dataSource(POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword())
            .locations("classpath:db/migration")
            .load();
    flyway.migrate();

    jdbcTemplate =
        new JdbcTemplate(
            new DriverManagerDataSource(
                POSTGRESQL.getJdbcUrl(), POSTGRESQL.getUsername(), POSTGRESQL.getPassword()));
  }

  @Test
  void enablesPinnedPgvectorVersionThroughFlyway() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("46");
    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT \"extversion\" FROM \"pg_extension\" WHERE \"extname\" = 'vector'",
                String.class))
        .isEqualTo("0.8.6");
  }

  /**
   * Prueba dimensión, distancia coseno e HNSW y después retira solo la proyección de prueba. La
   * extensión debe sobrevivir porque ese es el rollback lógico definido para producción.
   */
  @Test
  void supportsCosineDistanceHnswIndexAndLogicalRollback() {
    try {
      jdbcTemplate.execute(
          """
          CREATE TABLE "PgvectorCompatibilityProbe" (
            "id" integer PRIMARY KEY,
            "embedding" vector(3) NOT NULL
          )
          """);
      jdbcTemplate.update(
          """
          INSERT INTO "PgvectorCompatibilityProbe" ("id", "embedding")
          VALUES (1, '[1,0,0]'), (2, '[0,1,0]'), (3, '[0,0,1]')
          """);
      jdbcTemplate.execute(
          """
          CREATE INDEX "ixPgvectorCompatibilityProbeEmbeddingHnsw"
          ON "PgvectorCompatibilityProbe" USING hnsw ("embedding" vector_cosine_ops)
          """);

      Integer nearestId =
          jdbcTemplate.queryForObject(
              """
              SELECT "id"
              FROM "PgvectorCompatibilityProbe"
              ORDER BY "embedding" <=> '[0.9,0.1,0]'::vector
              LIMIT 1
              """,
              Integer.class);
      assertThat(nearestId).isOne();

      List<String> indexes =
          jdbcTemplate.queryForList(
              """
              SELECT "indexname"
              FROM "pg_indexes"
              WHERE "tablename" = 'PgvectorCompatibilityProbe'
              ORDER BY "indexname"
              """,
              String.class);
      assertThat(indexes)
          .contains("PgvectorCompatibilityProbe_pkey", "ixPgvectorCompatibilityProbeEmbeddingHnsw");

      assertThatThrownBy(
              () ->
                  jdbcTemplate.update(
                      """
                      INSERT INTO "PgvectorCompatibilityProbe" ("id", "embedding")
                      VALUES (4, '[1,0]')
                      """))
          .isInstanceOf(DataIntegrityViolationException.class);
    } finally {
      jdbcTemplate.execute("DROP TABLE IF EXISTS \"PgvectorCompatibilityProbe\"");
    }

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM \"pg_extension\" WHERE \"extname\" = 'vector')",
                Boolean.class))
        .isTrue();
  }
}
