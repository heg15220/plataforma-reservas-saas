package com.reserly.platform.demand.embedding;

import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UPSERT JDBC explícito para pgvector. Un checksum idéntico no toca {@code updatedAt}, por lo que
 * reejecutar un lote es observable como unchanged y no crea escrituras falsas.
 */
@Service
public class SubjectEmbeddingServiceImpl implements SubjectEmbeddingService {
  private final JdbcTemplate jdbcTemplate;

  public SubjectEmbeddingServiceImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  @Transactional
  public SubjectEmbeddingBatchResult persist(SubjectEmbeddingBatchRequest request) {
    int inserted = 0;
    int updated = 0;
    int unchanged = 0;
    for (SubjectEmbeddingWrite item : request.embeddings()) {
      String previous =
          jdbcTemplate.query(
              """
              SELECT "contentChecksum" FROM "SubjectEmbeddings"
              WHERE "subjectType" = ? AND "subjectId" = ? AND "locale" = ? AND "modelVersion" = ?
              """,
              result -> result.next() ? result.getString(1) : null,
              item.subjectType(),
              item.subjectId(),
              item.locale(),
              item.modelVersion());
      if (item.contentChecksum().equals(previous)) {
        unchanged++;
        continue;
      }
      jdbcTemplate.update(
          """
          INSERT INTO "SubjectEmbeddings" (
            "subjectType", "subjectId", "locale", "modelVersion", "dimensions",
            "contentChecksum", "embedding", "validFrom", "expiresAt"
          ) VALUES (?, ?, ?, ?, 384, ?, CAST(? AS vector), ?, ?)
          ON CONFLICT ("subjectType", "subjectId", "locale", "modelVersion") DO UPDATE SET
            "dimensions" = EXCLUDED."dimensions",
            "contentChecksum" = EXCLUDED."contentChecksum",
            "embedding" = EXCLUDED."embedding",
            "validFrom" = EXCLUDED."validFrom",
            "expiresAt" = EXCLUDED."expiresAt",
            "updatedAt" = CURRENT_TIMESTAMP
          """,
          item.subjectType(),
          item.subjectId(),
          item.locale(),
          item.modelVersion(),
          item.contentChecksum(),
          vectorLiteral(item.embedding()),
          Timestamp.from(item.validFrom()),
          item.expiresAt() == null ? null : Timestamp.from(item.expiresAt()));
      if (previous == null) {
        inserted++;
      } else {
        updated++;
      }
    }
    return new SubjectEmbeddingBatchResult(inserted, updated, unchanged);
  }

  private static String vectorLiteral(List<Double> values) {
    StringBuilder literal = new StringBuilder("[");
    for (int index = 0; index < values.size(); index++) {
      if (index > 0) {
        literal.append(',');
      }
      literal.append(Double.toString(values.get(index)));
    }
    return literal.append(']').toString();
  }
}
