package com.reserly.platform.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");

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
}
