package com.reserly.platform.resources.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de recursos de equipo acotada al propietario del local vigente. */
public interface EmployeeResourceDao extends JpaRepository<EmployeeResourceEntity, UUID> {

  @Query(
      """
      select resource from EmployeeResourceEntity resource
      where resource.venue.ownerUser.id = :ownerUserId
        and resource.venue.status <> 'archived'
        and resource.status <> 'archived'
      order by resource.publicAlias, resource.firstName, resource.lastName
      """)
  List<EmployeeResourceEntity> findAllOwnedActiveCatalog(@Param("ownerUserId") UUID ownerUserId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select resource from EmployeeResourceEntity resource
      where resource.id = :resourceId
        and resource.venue.ownerUser.id = :ownerUserId
        and resource.venue.status <> 'archived'
        and resource.status <> 'archived'
      """)
  Optional<EmployeeResourceEntity> findOwnedForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("resourceId") UUID resourceId);
}
