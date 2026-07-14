package com.reserly.platform.reservations.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** DAO del agregado con lecturas explícitas para capacidad y confirmación transaccional. */
public interface ReservationDao extends JpaRepository<ReservationEntity, UUID> {

  /**
   * Suma ocupación efectiva: reservas confirmadas en cualquier estado posterior y holds vigentes.
   * El llamador debe poseer el bloqueo pesimista de la franja antes de ejecutar esta consulta.
   */
  @Query(
      """
      select coalesce(sum(reservation.partySize), 0)
      from ReservationEntity reservation
      where reservation.timeSlot.id = :timeSlotId
        and (
          reservation.status in ('confirmed', 'attended', 'no_show', 'reported')
          or (reservation.status = 'hold' and reservation.holdExpiresAt > :now)
        )
      """)
  long sumOccupiedCapacity(
      @Param("timeSlotId") UUID timeSlotId, @Param("now") Instant now);

  /**
   * Suma la ocupación ajena al hold que se está confirmando. Debe ejecutarse con la franja
   * bloqueada para que la comprobación y la transición compartan una única instantánea.
   */
  @Query(
      """
      select coalesce(sum(reservation.partySize), 0)
      from ReservationEntity reservation
      where reservation.timeSlot.id = :timeSlotId
        and reservation.id <> :excludedReservationId
        and (
          reservation.status in ('confirmed', 'attended', 'no_show', 'reported')
          or (reservation.status = 'hold' and reservation.holdExpiresAt > :now)
        )
      """)
  long sumOccupiedCapacityExcluding(
      @Param("timeSlotId") UUID timeSlotId,
      @Param("excludedReservationId") UUID excludedReservationId,
      @Param("now") Instant now);

  /** Bloquea solo la reserva; la franja se adquiere después con un lock explícito separado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select reservation from ReservationEntity reservation
      where reservation.id = :reservationId
      """)
  Optional<ReservationEntity> findByIdForUpdate(
      @Param("reservationId") UUID reservationId);
}
