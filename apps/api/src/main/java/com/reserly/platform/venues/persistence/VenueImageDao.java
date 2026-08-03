package com.reserly.platform.venues.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de galería con consultas explícitas de propiedad y publicación. */
public interface VenueImageDao extends JpaRepository<VenueImageEntity, UUID> {

  @Query(
      """
      select image from VenueImageEntity image
      where image.venue.ownerUser.id = :ownerUserId
        and image.venue.status <> 'archived'
      order by image.position
      """)
  List<VenueImageEntity> findAllOwned(@Param("ownerUserId") UUID ownerUserId);

  /** Lista la galería de una ficha previamente autorizada por el servicio. */
  List<VenueImageEntity> findAllByVenueIdOrderByPosition(UUID venueId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select image from VenueImageEntity image
      where image.id = :imageId
        and image.venue.ownerUser.id = :ownerUserId
        and image.venue.status <> 'archived'
      """)
  Optional<VenueImageEntity> findOwnedForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("imageId") UUID imageId);

  /** Bloquea una imagen dentro de la ficha seleccionada para impedir cruces entre locales. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<VenueImageEntity> findByVenueIdAndId(UUID venueId, UUID imageId);

  @Query(
      """
      select image from VenueImageEntity image
      where image.id = :imageId and image.venue.status = 'published'
      """)
  Optional<VenueImageEntity> findPublished(@Param("imageId") UUID imageId);

  /** Devuelve la galería pública ordenada sin depender de identidad autenticada. */
  @Query(
      """
      select image from VenueImageEntity image
      where image.venue.id = :venueId
        and image.venue.status = 'published'
      order by image.position
      """)
  List<VenueImageEntity> findAllPublishedByVenueId(@Param("venueId") UUID venueId);
}
