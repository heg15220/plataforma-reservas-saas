package com.reserly.platform.demand.attribute.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a derivados ABSA vigentes y a la cola explícita de revisión humana. */
public interface ReviewAspectScoreDao extends JpaRepository<ReviewAspectScoreEntity, UUID> {
  @Query(
      """
      select score from ReviewAspectScoreEntity score
      where score.venueId = :venueId and score.demandAttributeId = :attributeId
        and score.expiresAt > :now and score.reviewStatus in ('machineAccepted', 'humanAccepted', 'humanCorrected')
      order by score.observedAt desc
      """)
  List<ReviewAspectScoreEntity> findAcceptedCurrent(
      @Param("venueId") UUID venueId,
      @Param("attributeId") UUID attributeId,
      @Param("now") Instant now);

  @Query(
      """
      select score from ReviewAspectScoreEntity score
      where score.reviewStatus = 'pendingHuman'
      order by score.createdAt asc
      """)
  List<ReviewAspectScoreEntity> findHumanReviewQueue();
}
