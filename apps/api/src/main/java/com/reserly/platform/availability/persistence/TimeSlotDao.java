package com.reserly.platform.availability.persistence;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de franjas de reserva acotada por propietario y local vigente. */
public interface TimeSlotDao extends JpaRepository<TimeSlotEntity, UUID> {

  /** Lista franjas propias de una fecha para el panel privado. */
  @Query(
      """
      select slot from TimeSlotEntity slot
      where slot.venue.ownerUser.id = :ownerUserId
        and slot.venue.status <> 'archived'
        and slot.date = :date
      order by slot.startsAt, slot.endsAt
      """)
  List<TimeSlotEntity> findAllOwnedByDate(
      @Param("ownerUserId") UUID ownerUserId, @Param("date") LocalDate date);

  /** Detecta solapes antes de crear una franja manual. */
  @Query(
      """
      select count(slot) > 0 from TimeSlotEntity slot
      where slot.venue.ownerUser.id = :ownerUserId
        and slot.venue.status <> 'archived'
        and slot.date = :date
        and slot.startsAt < :endsAt
        and slot.endsAt > :startsAt
      """)
  boolean existsOwnedOverlap(
      @Param("ownerUserId") UUID ownerUserId,
      @Param("date") LocalDate date,
      @Param("startsAt") LocalTime startsAt,
      @Param("endsAt") LocalTime endsAt);
}
