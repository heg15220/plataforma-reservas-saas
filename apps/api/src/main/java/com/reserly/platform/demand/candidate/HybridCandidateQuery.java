package com.reserly.platform.demand.candidate;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contexto minimizado de recuperación. El servicio vuelve a validar todas las restricciones aunque
 * el caller ya las haya aplicado.
 */
public record HybridCandidateQuery(
    String query,
    String locale,
    String categoryCode,
    double latitude,
    double longitude,
    int radiusMeters,
    LocalDate availabilityDate,
    UUID serviceId,
    int partySize,
    int limit,
    List<Double> queryEmbedding,
    Instant evaluatedAt) {

  public HybridCandidateQuery {
    if (query == null || query.isBlank() || query.length() > 240) {
      throw new IllegalArgumentException("CANDIDATE_QUERY_INVALID");
    }
    if (!"es".equals(locale) && !"en".equals(locale)) {
      throw new IllegalArgumentException("CANDIDATE_LOCALE_INVALID");
    }
    if (!"peluqueria".equals(categoryCode) && !"centro-de-estetica".equals(categoryCode)) {
      throw new IllegalArgumentException("CANDIDATE_CATEGORY_INVALID");
    }
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      throw new IllegalArgumentException("CANDIDATE_LOCATION_INVALID");
    }
    if (radiusMeters < 1 || radiusMeters > 25_000 || partySize != 1 || limit < 1 || limit > 100) {
      throw new IllegalArgumentException("CANDIDATE_LIMIT_INVALID");
    }
    if (availabilityDate == null || evaluatedAt == null) {
      throw new IllegalArgumentException("CANDIDATE_TIME_INVALID");
    }
    if (queryEmbedding != null
        && (queryEmbedding.size() != 384
            || queryEmbedding.stream()
                .anyMatch(value -> value == null || !Double.isFinite(value)))) {
      throw new IllegalArgumentException("CANDIDATE_VECTOR_INVALID");
    }
  }
}
