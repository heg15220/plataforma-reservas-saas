package com.reserly.platform.demand.attribute.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consulta evidencias vigentes sin eliminar las expiradas ni contradictorias. */
public interface VenueAttributeEvidenceDao
    extends JpaRepository<VenueAttributeEvidenceEntity, UUID> {
  @Query(
      """
      select evidence from VenueAttributeEvidenceEntity evidence
      where evidence.venueId = :venueId and evidence.attributeId = :attributeId
        and (evidence.expiresAt is null or evidence.expiresAt > :now)
      order by evidence.observedAt, evidence.id
      """)
  List<VenueAttributeEvidenceEntity> findActive(
      @Param("venueId") UUID venueId,
      @Param("attributeId") UUID attributeId,
      @Param("now") Instant now);
}
