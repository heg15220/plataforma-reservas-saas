package com.reserly.platform.venues.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de perfiles siempre acotada por el propietario autenticado. */
public interface VenueDao extends JpaRepository<VenueEntity, UUID> {

  /** Carga el perfil vigente y su categoría para lectura privada. */
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      where venue.ownerUser.id = :ownerUserId
        and venue.status <> 'archived'
      """)
  Optional<VenueEntity> findCurrentByOwnerUserId(@Param("ownerUserId") UUID ownerUserId);

  /** Serializa actualizaciones y archivo del único perfil vigente del propietario. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select venue
      from VenueEntity venue
      join fetch venue.category
      where venue.ownerUser.id = :ownerUserId
        and venue.status <> 'archived'
      """)
  Optional<VenueEntity> findCurrentByOwnerUserIdForUpdate(@Param("ownerUserId") UUID ownerUserId);

  /** Resuelve exclusivamente imágenes de perfiles publicados para entrega anónima. */
  @Query(
      """
      select venue
      from VenueEntity venue
      where venue.id = :venueId
        and venue.status = 'published'
        and venue.mainImageObjectKey is not null
      """)
  Optional<VenueEntity> findPublishedWithMainImage(@Param("venueId") UUID venueId);
}
