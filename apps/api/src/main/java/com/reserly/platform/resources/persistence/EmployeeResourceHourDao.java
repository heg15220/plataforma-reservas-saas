package com.reserly.platform.resources.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia del horario semanal de recursos, siempre acotada al propietario del local. */
public interface EmployeeResourceHourDao extends JpaRepository<EmployeeResourceHourEntity, UUID> {

  @Query(
      """
      select hour from EmployeeResourceHourEntity hour
      where hour.employeeResource.id = :resourceId
        and hour.employeeResource.venue.ownerUser.id = :ownerUserId
        and hour.employeeResource.venue.status <> 'archived'
        and hour.employeeResource.status <> 'archived'
      order by hour.weekday
      """)
  List<EmployeeResourceHourEntity> findWeeklyHours(
      @Param("ownerUserId") UUID ownerUserId, @Param("resourceId") UUID resourceId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select hour from EmployeeResourceHourEntity hour
      where hour.employeeResource.id = :resourceId
        and hour.employeeResource.venue.ownerUser.id = :ownerUserId
        and hour.employeeResource.venue.status <> 'archived'
        and hour.employeeResource.status <> 'archived'
      order by hour.weekday
      """)
  List<EmployeeResourceHourEntity> findWeeklyHoursForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("resourceId") UUID resourceId);

  /**
   * Carga horarios utilizables en el canal publico para un local publicado y un dia semanal.
   * Recursos internos, inactivos o no visibles quedan excluidos antes de proyectar la respuesta.
   */
  @Query(
      """
      select hour from EmployeeResourceHourEntity hour
      join fetch hour.employeeResource resource
      where resource.venue.id = :venueId
        and resource.venue.status = 'published'
        and resource.status = 'active'
        and resource.publicVisibility = true
        and hour.weekday = :weekday
        and hour.available = true
      """)
  List<EmployeeResourceHourEntity> findPublishedAvailableHours(
      @Param("venueId") UUID venueId, @Param("weekday") int weekday);
}
