package com.reserly.platform.reservations.persistence;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Lectura de franjas estrictamente necesaria para iniciar reservas públicas. */
public interface ReservationTimeSlotDao extends Repository<TimeSlotEntity, UUID> {

  /** Devuelve la franja solo si pertenece al local indicado y este continúa publicado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select slot from TimeSlotEntity slot
      join fetch slot.venue venue
      where slot.id = :timeSlotId
        and venue.id = :venueId
        and venue.status = 'published'
      """)
  Optional<TimeSlotEntity> findPublishedForUpdate(
      @Param("venueId") UUID venueId, @Param("timeSlotId") UUID timeSlotId);
}
