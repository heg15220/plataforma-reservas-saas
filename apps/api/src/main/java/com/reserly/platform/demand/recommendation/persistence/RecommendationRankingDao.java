package com.reserly.platform.demand.recommendation.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso al orden final reproducible de una petición. */
public interface RecommendationRankingDao extends JpaRepository<RecommendationRankingEntity, UUID> {

  /** Devuelve el ranking exactamente en el orden que recibió el consumidor. */
  @Query(
      """
      select ranking
      from RecommendationRankingEntity ranking
      where ranking.recommendationRequest.id = :requestId
      order by ranking.finalPosition
      """)
  List<RecommendationRankingEntity> findByRequestIdOrdered(
      @Param("requestId") UUID recommendationRequestId);
}
