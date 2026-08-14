package com.reserly.platform.demand.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.configuration.ReserlyPostgreSqlContainerProvider;
import java.time.Instant;
import java.util.ArrayList;
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

/** Verifica físicamente pgvector, restricciones e idempotencia del UPSERT de V52. */
@Testcontainers
class SubjectEmbeddingPersistenceIntegrationTests {
  @Container
  private static final PostgreSQLContainer<?> POSTGRESQL =
      (PostgreSQLContainer<?>) new ReserlyPostgreSqlContainerProvider().newInstance();

  private static JdbcTemplate jdbc;
  private static SubjectEmbeddingService service;

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
    service = new SubjectEmbeddingServiceImpl(jdbc);
  }

  @Test
  void insertsSkipsSameChecksumAndUpdatesChangedContent() {
    UUID subjectId = UUID.randomUUID();
    Instant now = Instant.parse("2026-08-14T10:00:00Z");
    SubjectEmbeddingWrite first = write(subjectId, "venue", "a".repeat(64), now, null, 1.0);

    assertThat(service.persist(new SubjectEmbeddingBatchRequest(List.of(first))))
        .isEqualTo(new SubjectEmbeddingBatchResult(1, 0, 0));
    Instant originalUpdatedAt =
        jdbc.queryForObject(
            "SELECT \"updatedAt\" FROM \"SubjectEmbeddings\" WHERE \"subjectId\" = ?",
            Instant.class,
            subjectId);
    assertThat(service.persist(new SubjectEmbeddingBatchRequest(List.of(first))))
        .isEqualTo(new SubjectEmbeddingBatchResult(0, 0, 1));
    assertThat(
            jdbc.queryForObject(
                "SELECT \"updatedAt\" FROM \"SubjectEmbeddings\" WHERE \"subjectId\" = ?",
                Instant.class,
                subjectId))
        .isEqualTo(originalUpdatedAt);

    SubjectEmbeddingWrite changed = write(subjectId, "venue", "b".repeat(64), now, null, 0.5);
    assertThat(service.persist(new SubjectEmbeddingBatchRequest(List.of(changed))))
        .isEqualTo(new SubjectEmbeddingBatchResult(0, 1, 0));
    assertThat(
            jdbc.queryForObject(
                "SELECT \"contentChecksum\" FROM \"SubjectEmbeddings\" WHERE \"subjectId\" = ?",
                String.class,
                subjectId))
        .isEqualTo("b".repeat(64));
  }

  @Test
  void databaseRejectsPersistentQueryAndWrongDimensions() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO "SubjectEmbeddings" (
                      "subjectType", "subjectId", "locale", "modelVersion", "dimensions",
                      "contentChecksum", "embedding", "validFrom"
                    ) VALUES (
                      'query', ?, 'es', 'model-v1', 384, ?, CAST(? AS vector), CURRENT_TIMESTAMP
                    )
                    """,
                    UUID.randomUUID(),
                    "c".repeat(64),
                    vector(384, 0.0)))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("ckSubjectEmbeddingsValidity");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    INSERT INTO "SubjectEmbeddings" (
                      "subjectType", "subjectId", "locale", "modelVersion", "dimensions",
                      "contentChecksum", "embedding", "validFrom"
                    ) VALUES (
                      'venue', ?, 'es', 'model-v1', 384, ?, CAST(? AS vector), CURRENT_TIMESTAMP
                    )
                    """,
                    UUID.randomUUID(),
                    "d".repeat(64),
                    vector(383, 0.0)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private SubjectEmbeddingWrite write(
      UUID id, String type, String checksum, Instant validFrom, Instant expiresAt, double first) {
    List<Double> values = new ArrayList<>(java.util.Collections.nCopies(384, 0.0));
    values.set(0, first);
    return new SubjectEmbeddingWrite(
        id, type, "es", "multilingual-e5-small-v1", checksum, values, validFrom, expiresAt);
  }

  private String vector(int dimensions, double value) {
    return "["
        + String.join(",", java.util.Collections.nCopies(dimensions, Double.toString(value)))
        + "]";
  }
}
