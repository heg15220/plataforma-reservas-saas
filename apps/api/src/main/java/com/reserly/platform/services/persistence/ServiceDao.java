package com.reserly.platform.services.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de servicios siempre acotada al propietario del local vigente. */
public interface ServiceDao extends JpaRepository<ServiceEntity, UUID> {

  @Query(
      """
      select service from ServiceEntity service
      where service.venue.ownerUser.id = :ownerUserId
        and service.venue.status <> 'archived'
      order by service.name
      """)
  List<ServiceEntity> findAllOwned(@Param("ownerUserId") UUID ownerUserId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select service from ServiceEntity service
      where service.id = :serviceId
        and service.venue.ownerUser.id = :ownerUserId
        and service.venue.status <> 'archived'
      """)
  Optional<ServiceEntity> findOwnedForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("serviceId") UUID serviceId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select service from ServiceEntity service
      left join fetch service.compatibleResources
      where service.id = :serviceId
        and service.venue.ownerUser.id = :ownerUserId
        and service.venue.status <> 'archived'
      """)
  Optional<ServiceEntity> findOwnedWithResourcesForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("serviceId") UUID serviceId);
}
