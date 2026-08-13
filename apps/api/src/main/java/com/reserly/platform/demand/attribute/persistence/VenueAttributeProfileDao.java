package com.reserly.platform.demand.attribute.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Mantiene una única proyección vigente por pareja local/atributo. */
public interface VenueAttributeProfileDao extends JpaRepository<VenueAttributeProfileEntity, UUID> {
  Optional<VenueAttributeProfileEntity> findByVenueIdAndAttributeId(UUID venueId, UUID attributeId);
}
