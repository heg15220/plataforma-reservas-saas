package com.reserly.platform.availability.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
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

  /** Resuelve el horario semanal aplicable a una fecha concreta por weekday ISO. */
  @Query(
      """
      select hour from VenueOpeningHourEntity hour
      where hour.venue.ownerUser.id = :ownerUserId
        and hour.venue.status <> 'archived'
        and hour.weekday = :weekday
      """)
  Optional<VenueOpeningHourEntity> findOwnedByWeekday(
      @Param("ownerUserId") UUID ownerUserId, @Param("weekday") int weekday);

  /** Resuelve el horario semanal público de un local publicado por weekday ISO. */
  @Query(
      """
      select hour from VenueOpeningHourEntity hour
      where hour.venue.id = :venueId
        and hour.venue.status = 'published'
        and hour.weekday = :weekday
      """)
  Optional<VenueOpeningHourEntity> findPublishedByVenueIdAndWeekday(
      @Param("venueId") UUID venueId, @Param("weekday") int weekday);
}
