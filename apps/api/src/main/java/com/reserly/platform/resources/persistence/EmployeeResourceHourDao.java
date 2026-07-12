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
}
