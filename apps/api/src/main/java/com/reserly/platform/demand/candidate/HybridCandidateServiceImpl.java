package com.reserly.platform.demand.candidate;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperación híbrida en una sola fotografía READ ONLY.
 *
 * <p>Publicación, vertical, radio, servicio, capacidad y bloqueos son filtros SQL previos al score
 * y no pueden ser anulados por ML.
 */
@Service
public class HybridCandidateServiceImpl implements HybridCandidateService {
  static final String POLICY_TEXT_ONLY = "hybrid-retrieval-text-v1";
  static final String POLICY_WITH_VECTOR = "hybrid-retrieval-vector-v1";

  private final JdbcTemplate jdbcTemplate;
  private final HybridCandidateProperties properties;

  public HybridCandidateServiceImpl(
      JdbcTemplate jdbcTemplate, HybridCandidateProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.properties = properties;
  }

  @Override
  @Transactional(readOnly = true)
  public List<HybridCandidate> generate(HybridCandidateQuery query) {
    boolean useVector = properties.vectorEnabled() && query.queryEmbedding() != null;
    String vectorJoin =
        useVector
            ? """
              LEFT JOIN "SubjectEmbeddings" venue_embedding
                ON venue_embedding."subjectType" = 'venue'
               AND venue_embedding."subjectId" = venue."id"
               AND venue_embedding."locale" = ?
               AND venue_embedding."modelVersion" = ?
               AND venue_embedding."validFrom" <= ?
               AND (venue_embedding."expiresAt" IS NULL OR venue_embedding."expiresAt" > ?)
              LEFT JOIN "SubjectEmbeddings" service_embedding
                ON service_embedding."subjectType" = 'service'
               AND service_embedding."subjectId" = service."id"
               AND service_embedding."locale" = ?
               AND service_embedding."modelVersion" = ?
               AND service_embedding."validFrom" <= ?
               AND (service_embedding."expiresAt" IS NULL OR service_embedding."expiresAt" > ?)
              """
            : "";
    String vectorProjection =
        useVector
            ? """
              GREATEST(
                COALESCE(1.0 - (venue_embedding."embedding" <=> CAST(? AS vector)), 0.0),
                COALESCE(1.0 - (service_embedding."embedding" <=> CAST(? AS vector)), 0.0),
                0.0
              )
              """
            : "0.0";
    String scoreExpression =
        useVector
            ? "(0.55 * \"fullTextScore\" + 0.30 * \"trigramScore\" + 0.15 * \"vectorScore\")"
            : "(0.65 * \"fullTextScore\" + 0.35 * \"trigramScore\")";
    String sql = baseSql(vectorJoin, vectorProjection, scoreExpression);

    java.util.ArrayList<Object> arguments = new java.util.ArrayList<>();
    arguments.add(query.longitude());
    arguments.add(query.latitude());
    arguments.add(query.query());
    arguments.add(query.query());
    arguments.add(query.query());
    if (useVector) {
      arguments.add(vectorLiteral(query.queryEmbedding()));
      arguments.add(vectorLiteral(query.queryEmbedding()));
      arguments.add(query.locale());
      arguments.add(properties.modelVersion());
      arguments.add(Timestamp.from(query.evaluatedAt()));
      arguments.add(Timestamp.from(query.evaluatedAt()));
      arguments.add(query.locale());
      arguments.add(properties.modelVersion());
      arguments.add(Timestamp.from(query.evaluatedAt()));
      arguments.add(Timestamp.from(query.evaluatedAt()));
    }
    arguments.add(Date.valueOf(query.availabilityDate()));
    arguments.add(Timestamp.from(query.evaluatedAt()));
    arguments.add(query.partySize());
    arguments.add(query.categoryCode());
    arguments.add(query.serviceId());
    arguments.add(query.serviceId());
    arguments.add(query.longitude());
    arguments.add(query.latitude());
    arguments.add(query.radiusMeters());
    arguments.add(query.limit());

    String policy = useVector ? POLICY_WITH_VECTOR : POLICY_TEXT_ONLY;
    return jdbcTemplate.query(
        sql,
        (result, row) ->
            new HybridCandidate(
                result.getObject("venueId", UUID.class),
                result.getObject("serviceId", UUID.class),
                result.getString("categoryCode"),
                result.getInt("distanceMeters"),
                result.getInt("availableSlotCount"),
                result.getDouble("fullTextScore"),
                result.getDouble("trigramScore"),
                result.getDouble("vectorScore"),
                result.getDouble("retrievalScore"),
                policy),
        arguments.toArray());
  }

  private static String baseSql(
      String vectorJoin, String vectorProjection, String scoreExpression) {
    return """
        WITH scored AS (
          SELECT venue."id" AS "venueId", service."id" AS "serviceId",
            category."slug" AS "categoryCode",
            round(ST_Distance(
              venue."location", ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography
            ))::int
              AS "distanceMeters",
            ts_rank_cd(
              to_tsvector('simple', lower("reserlyUnaccent"(
                venue."name" || ' ' || coalesce(venue."description", '') || ' '
                || service."name" || ' ' || coalesce(service."description", '')
              ))),
              websearch_to_tsquery('simple', lower("reserlyUnaccent"(?)))
            )::double precision AS "fullTextScore",
            GREATEST(
              similarity(lower("reserlyUnaccent"(venue."name")), lower("reserlyUnaccent"(?))),
              similarity(lower("reserlyUnaccent"(service."name")), lower("reserlyUnaccent"(?)))
            )::double precision AS "trigramScore",
            %s::double precision AS "vectorScore",
            availability."availableSlotCount"
          FROM "Venues" venue
          JOIN "Categories" category ON category."id" = venue."categoryId"
          JOIN "Services" service ON service."venueId" = venue."id"
          %s
          JOIN LATERAL (
            SELECT count(*)::int AS "availableSlotCount"
            FROM "TimeSlots" slot
            WHERE slot."venueId" = venue."id"
              AND slot."serviceId" = service."id"
              AND slot."date" = ?
              AND slot."status" = 'available'
              AND NOT EXISTS (
                SELECT 1 FROM "AvailabilityBlocks" block
                WHERE block."venueId" = venue."id" AND block."date" = slot."date"
                  AND (block."scope" = 'venue'
                    OR (block."scope" = 'service' AND block."serviceId" = service."id")
                    OR (block."scope" = 'slot' AND block."timeSlotId" = slot."id"))
              )
              AND slot."capacity" - COALESCE((
                SELECT sum(reservation."partySize") FROM "Reservations" reservation
                WHERE reservation."timeSlotId" = slot."id"
                  AND (reservation."status" IN ('pending_confirmation', 'confirmed')
                    OR (reservation."status" = 'hold' AND reservation."holdExpiresAt" > ?))
              ), 0) >= ?
          ) availability ON availability."availableSlotCount" > 0
          WHERE venue."status" = 'published'
            AND venue."manualAvailabilityStatus" <> 'unavailable'
            AND venue."location" IS NOT NULL
            AND category."isActive" = true AND category."slug" = ?
            AND category."slug" IN ('peluqueria', 'centro-de-estetica')
            AND service."isActive" = true AND service."capacityRequired" = 1
            AND (?::uuid IS NULL OR service."id" = ?::uuid)
            AND ST_DWithin(
              venue."location", ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography, ?
            )
        ), ranked AS (
          SELECT *, %s AS "retrievalScore",
            row_number() OVER (
              PARTITION BY "venueId" ORDER BY %s DESC, "serviceId"
            ) AS venue_rank
          FROM scored
          WHERE "fullTextScore" > 0 OR "trigramScore" >= 0.1 OR "vectorScore" > 0
        )
        SELECT "venueId", "serviceId", "categoryCode", "distanceMeters",
          "availableSlotCount", "fullTextScore", "trigramScore", "vectorScore",
          "retrievalScore"
        FROM ranked WHERE venue_rank = 1
        ORDER BY "retrievalScore" DESC, "distanceMeters", "venueId"
        LIMIT ?
        """
        .formatted(vectorProjection, vectorJoin, scoreExpression, scoreExpression);
  }

  private static String vectorLiteral(List<Double> values) {
    return "["
        + values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
        + "]";
  }
}
