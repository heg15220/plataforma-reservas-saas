package com.reserly.platform.availability.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
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

  /** Lista franjas de un local publicado para el calendario público de una fecha. */
  @Query(
      """
      select slot from TimeSlotEntity slot
      where slot.venue.id = :venueId
        and slot.venue.status = 'published'
        and slot.date = :date
      order by slot.startsAt, slot.endsAt
      """)
  List<TimeSlotEntity> findPublishedByVenueIdAndDate(
      @Param("venueId") UUID venueId, @Param("date") LocalDate date);

  /** Indica si existen huecos futuros disponibles después de la fecha consultada. */
  @Query(
      """
      select count(slot) > 0 from TimeSlotEntity slot
      where slot.venue.id = :venueId
        and slot.venue.status = 'published'
        and slot.date > :date
        and slot.status = 'available'
      """)
  boolean existsPublishedAvailableAfter(
      @Param("venueId") UUID venueId, @Param("date") LocalDate date);

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

  /** Bloquea una franja propia antes de cambiar capacidad o estado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select slot from TimeSlotEntity slot
      where slot.id = :slotId
        and slot.venue.ownerUser.id = :ownerUserId
        and slot.venue.status <> 'archived'
      """)
  Optional<TimeSlotEntity> findOwnedForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("slotId") UUID slotId);

  /** Cambia a no disponible todas las franjas no bloqueadas de una fecha cerrada. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update TimeSlotEntity slot
      set slot.status = 'unavailable',
          slot.updatedAt = :updatedAt
      where slot.venue.ownerUser.id = :ownerUserId
        and slot.venue.status <> 'archived'
        and slot.date = :date
        and slot.status <> 'blocked'
      """)
  int markOwnedDayUnavailable(
      @Param("ownerUserId") UUID ownerUserId,
      @Param("date") LocalDate date,
      @Param("updatedAt") Instant updatedAt);

  /** Restaura como disponibles las franjas que quedaron no disponibles por un cierre diario. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update TimeSlotEntity slot
      set slot.status = 'available',
          slot.updatedAt = :updatedAt
      where slot.venue.ownerUser.id = :ownerUserId
        and slot.venue.status <> 'archived'
        and slot.date = :date
        and slot.status = 'unavailable'
      """)
  int reopenOwnedDayUnavailableSlots(
      @Param("ownerUserId") UUID ownerUserId,
      @Param("date") LocalDate date,
      @Param("updatedAt") Instant updatedAt);
}
