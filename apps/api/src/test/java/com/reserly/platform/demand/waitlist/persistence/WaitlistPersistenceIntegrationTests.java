package com.reserly.platform.demand.waitlist.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.configuration.ReserlyPostgreSqlContainerProvider;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifica en PostgreSQL real tablas, restricciones e índices operativos creados por V59. */
@Testcontainers
class WaitlistPersistenceIntegrationTests {

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
  void createsAuthoritativeWaitlistTablesAndOperationalIndexes() {
    List<String> tables =
        jdbcTemplate.queryForList(
            """
            SELECT "table_name"
            FROM "information_schema"."tables"
            WHERE "table_schema" = current_schema()
              AND "table_name" IN ('WaitlistEntries', 'WaitlistOffers')
            ORDER BY "table_name"
            """,
            String.class);
    assertThat(tables).containsExactly("WaitlistEntries", "WaitlistOffers");

    List<String> indexes =
        jdbcTemplate.queryForList(
            """
            SELECT "indexname"
            FROM "pg_indexes"
            WHERE "schemaname" = current_schema()
              AND "tablename" IN ('WaitlistEntries', 'WaitlistOffers')
            """,
            String.class);
    assertThat(indexes)
        .contains(
            "uqWaitlistEntriesVenueIdempotency",
            "uqWaitlistOffersRequestEntry",
            "uqWaitlistOffersTokenHash",
            "ixWaitlistEntriesSlotQueue",
            "ixWaitlistOffersActivation");
  }

  @Test
  void storesNoRawOfferTokenInSchema() {
    List<String> suspiciousColumns =
        jdbcTemplate.queryForList(
            """
            SELECT "column_name"
            FROM "information_schema"."columns"
            WHERE "table_schema" = current_schema()
              AND "table_name" = 'WaitlistOffers'
              AND lower("column_name") IN ('offertoken', 'token', 'secret')
            """,
            String.class);
    assertThat(suspiciousColumns).isEmpty();
    assertThat(
            jdbcTemplate.queryForObject(
                """
                SELECT "character_maximum_length"
                FROM "information_schema"."columns"
                WHERE "table_schema" = current_schema()
                  AND "table_name" = 'WaitlistOffers'
                  AND "column_name" = 'offerTokenHash'
                """,
                Integer.class))
        .isEqualTo(64);
  }
}
