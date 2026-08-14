package com.reserly.platform.demand.quality;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audita el dataset fundacional sin extraer filas ni materializar datos personales.
 *
 * <p>Las consultas devuelven exclusivamente contadores. El escáner de PII busca claves prohibidas,
 * email y teléfonos plausibles incluso si una escritura ajena a la API burló la validación previa.
 */
@Service
public class DemandDatasetQualityService {
  private static final String EMAIL_PATTERN = "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}";
  private static final String PHONE_PATTERN = "\\+?[0-9][0-9 ()\\-]{7,}[0-9]";
  private static final Duration MAX_WINDOW = Duration.ofDays(31);

  private final JdbcTemplate jdbc;
  private final Clock clock;

  public DemandDatasetQualityService(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  /** Ejecuta la auditoría para una ventana UTC de hasta 31 días. */
  @Transactional(readOnly = true)
  public DemandDatasetQualityReport audit(Duration window) {
    if (window.isNegative() || window.isZero() || window.compareTo(MAX_WINDOW) > 0) {
      throw new IllegalArgumentException("La ventana debe estar entre 1 segundo y 31 días");
    }
    Instant now = clock.instant();
    Instant start = now.minus(window);
    QualityCounters counters =
        jdbc.queryForObject(
            """
            SELECT
              count(*) AS total,
              count(*) FILTER (WHERE
                event."eventId" IS NULL OR event."requestId" IS NULL
                OR event."eventType" IS NULL OR event."eventFamily" IS NULL
                OR event."producer" IS NULL OR event."purpose" IS NULL
                OR event."contextJson" IS NULL OR event."retentionExpiresAt" IS NULL
              ) AS incomplete,
              COALESCE((
                SELECT sum(duplicates.amount - 1)
                FROM (
                  SELECT count(*) AS amount
                  FROM "BehaviorEvents" duplicate_event
                  WHERE duplicate_event."receivedAt" >= ?
                  GROUP BY duplicate_event."eventId" HAVING count(*) > 1
                ) duplicates
              ), 0) AS duplicates,
              count(*) FILTER (WHERE
                event."occurredAt" > event."receivedAt"
                OR event."receivedAt" > event."createdAt"
                OR event."retentionExpiresAt" <= event."receivedAt"
              ) AS temporal,
              count(*) FILTER (WHERE
                (event."anonymousIdentityId" IS NOT NULL OR event."customerIdentityId" IS NOT NULL)
                AND (
                  event."consentVersion" IS NULL
                  OR (anonymous."id" IS NOT NULL AND (
                    anonymous."personalizationConsentedAt" > event."occurredAt"
                    OR anonymous."personalizationRevokedAt" <= event."occurredAt"
                  ))
                  OR (customer."id" IS NOT NULL AND (
                    customer."personalizationConsentedAt" > event."occurredAt"
                    OR customer."personalizationRevokedAt" <= event."occurredAt"
                  ))
                )
              ) AS consent,
              count(*) FILTER (WHERE
                jsonb_exists_any(event."contextJson", ARRAY[
                  'email', 'phone', 'ip', 'ipAddress', 'userAgent', 'fingerprint',
                  'reviewText', 'formAnswers', 'rawQuery', 'payload'
                ])
                OR event."contextJson"::text ~* ?
                OR event."contextJson"::text ~ ?
              ) AS pii
            FROM "BehaviorEvents" event
            LEFT JOIN "AnonymousIdentities" anonymous
              ON anonymous."id" = event."anonymousIdentityId"
            LEFT JOIN "CustomerIdentities" customer
              ON customer."id" = event."customerIdentityId"
            WHERE event."receivedAt" >= ?
            """,
            (resultSet, rowNumber) ->
                new QualityCounters(
                    resultSet.getLong("total"),
                    resultSet.getLong("incomplete"),
                    resultSet.getLong("duplicates"),
                    resultSet.getLong("temporal"),
                    resultSet.getLong("consent"),
                    resultSet.getLong("pii")),
            Timestamp.from(start),
            EMAIL_PATTERN,
            PHONE_PATTERN,
            Timestamp.from(start));
    if (counters == null) throw new IllegalStateException("La auditoría no devolvió contadores");
    return new DemandDatasetQualityReport(
        now,
        start,
        counters.total(),
        counters.incomplete(),
        counters.duplicates(),
        counters.temporal(),
        counters.consent(),
        counters.pii());
  }

  private record QualityCounters(
      long total, long incomplete, long duplicates, long temporal, long consent, long pii) {}
}
