package com.reserly.platform.venues.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia explícita de pestañas acotada por propietario y local vigente. */
public interface VenueCustomTabDao extends JpaRepository<VenueCustomTabEntity, UUID> {

  @Query(
      """
      select tab from VenueCustomTabEntity tab
      where tab.venue.ownerUser.id = :ownerUserId
        and tab.venue.status <> 'archived'
      order by tab.position
      """)
  List<VenueCustomTabEntity> findAllOwned(@Param("ownerUserId") UUID ownerUserId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select tab from VenueCustomTabEntity tab
      where tab.id = :tabId
        and tab.venue.ownerUser.id = :ownerUserId
        and tab.venue.status <> 'archived'
      """)
  Optional<VenueCustomTabEntity> findOwnedForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("tabId") UUID tabId);
}
