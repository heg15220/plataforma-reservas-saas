package com.reserly.platform.demand.recommendation.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso ordenado al conjunto de alternativas de una recomendación. */
public interface RecommendationCandidateDao
    extends JpaRepository<RecommendationCandidateEntity, UUID> {

  /** Recupera el conjunto completo en el orden previo, incluido lo descartado. */
  @Query(
      """
      select candidate
      from RecommendationCandidateEntity candidate
      where candidate.recommendationRequest.id = :requestId
      order by candidate.sourcePosition
      """)
  List<RecommendationCandidateEntity> findAllByRequestIdOrdered(
      @Param("requestId") UUID recommendationRequestId);

  /** Recupera únicamente alternativas elegibles que pueden participar en ranking o impresión. */
  @Query(
      """
      select candidate
      from RecommendationCandidateEntity candidate
      where candidate.recommendationRequest.id = :requestId
        and candidate.eligibilityStatus = 'eligible'
      order by candidate.sourcePosition
      """)
  List<RecommendationCandidateEntity> findEligibleByRequestIdOrdered(
      @Param("requestId") UUID recommendationRequestId);
}
