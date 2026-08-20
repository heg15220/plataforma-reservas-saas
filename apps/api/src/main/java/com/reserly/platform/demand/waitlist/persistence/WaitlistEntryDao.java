package com.reserly.platform.demand.waitlist.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso autoritativo a entradas, incluida la transición serializada de aceptación. */
public interface WaitlistEntryDao extends JpaRepository<WaitlistEntryEntity, UUID> {

  Optional<WaitlistEntryEntity> findByVenueIdAndIdempotencyKey(UUID venueId, String idempotencyKey);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select entry from WaitlistEntryEntity entry where entry.id = :entryId")
  Optional<WaitlistEntryEntity> findByIdForUpdate(@Param("entryId") UUID entryId);
}
