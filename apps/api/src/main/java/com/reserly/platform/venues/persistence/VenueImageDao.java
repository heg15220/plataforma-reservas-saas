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

  @Query(
      """
      select image from VenueImageEntity image
      where image.id = :imageId and image.venue.status = 'published'
      """)
  Optional<VenueImageEntity> findPublished(@Param("imageId") UUID imageId);
}
