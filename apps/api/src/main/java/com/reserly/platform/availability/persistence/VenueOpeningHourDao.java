package com.reserly.platform.availability.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia del horario semanal acotada al local vigente del propietario. */
public interface VenueOpeningHourDao extends JpaRepository<VenueOpeningHourEntity, UUID> {

  /** Lista el horario semanal privado del propietario autenticado en orden ISO lunes-domingo. */
  @Query(
      """
      select hour from VenueOpeningHourEntity hour
      where hour.venue.ownerUser.id = :ownerUserId
        and hour.venue.status <> 'archived'
      order by hour.weekday
      """)
  List<VenueOpeningHourEntity> findAllOwned(@Param("ownerUserId") UUID ownerUserId);

  /**
   * Bloquea las filas existentes para sustituir el snapshot semanal sin carreras entre ediciones.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select hour from VenueOpeningHourEntity hour
      where hour.venue.ownerUser.id = :ownerUserId
        and hour.venue.status <> 'archived'
      order by hour.weekday
      """)
  List<VenueOpeningHourEntity> findAllOwnedForUpdate(@Param("ownerUserId") UUID ownerUserId);
}
