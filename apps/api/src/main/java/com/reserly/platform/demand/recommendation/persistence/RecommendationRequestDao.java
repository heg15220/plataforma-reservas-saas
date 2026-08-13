package com.reserly.platform.demand.recommendation.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso idempotente y temporal a sobres de recomendación auditables. */
public interface RecommendationRequestDao extends JpaRepository<RecommendationRequestEntity, UUID> {

  /** Recupera una decisión existente para resolver un reintento sin recalcularla. */
  @Query("select request from RecommendationRequestEntity request where request.requestId = :id")
  Optional<RecommendationRequestEntity> findByRequestId(@Param("id") UUID requestId);

  /** Selecciona lotes de retención sin cargar candidatos ni rankings. */
  @Query(
      """
      select request
      from RecommendationRequestEntity request
      where request.retentionExpiresAt <= :cutoff
      order by request.retentionExpiresAt, request.requestId
      """)
  List<RecommendationRequestEntity> findRetentionExpired(
      @Param("cutoff") Instant cutoff, Pageable pageable);
}
