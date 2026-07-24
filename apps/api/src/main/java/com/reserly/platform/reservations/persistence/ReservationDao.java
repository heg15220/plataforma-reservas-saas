package com.reserly.platform.reservations.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** DAO del agregado con lecturas explícitas para capacidad y confirmación transaccional. */
public interface ReservationDao extends JpaRepository<ReservationEntity, UUID> {

  /**
   * Expira en una sola sentencia los holds cuyo plazo terminó estrictamente antes del instante
   * recibido. La condición de estado hace la operación idempotente y evita sobrescribir una
   * confirmación concurrente.
   *
   * @param now instante UTC que se usa tanto como frontera como fecha de actualización
   * @return número de holds que cambiaron a {@code expired}
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      update ReservationEntity reservation
      set reservation.status = 'expired', reservation.updatedAt = :now
      where reservation.status = 'hold'
        and reservation.holdExpiresAt < :now
      """)
  int expireHoldsBefore(@Param("now") Instant now);

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
  long sumOccupiedCapacity(@Param("timeSlotId") UUID timeSlotId, @Param("now") Instant now);

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
  Optional<ReservationEntity> findByIdForUpdate(@Param("reservationId") UUID reservationId);

  /** Resuelve únicamente la huella de gestión y carga el local para la proyección pública. */
  @Query(
      "select reservation from ReservationEntity reservation join fetch reservation.venue "
          + "where reservation.secureTokenHash = :tokenHash")
  Optional<ReservationEntity> findBySecureTokenHash(@Param("tokenHash") String tokenHash);

  /** Bloquea la reserva asociada a la huella antes de validar plazo y cambiar estado. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select reservation from ReservationEntity reservation join fetch reservation.venue "
          + "where reservation.secureTokenHash = :tokenHash")
  Optional<ReservationEntity> findBySecureTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
