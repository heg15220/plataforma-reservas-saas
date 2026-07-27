package com.reserly.platform.reservations.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** DAO del agregado con lecturas explícitas para capacidad y confirmación transaccional. */
public interface ReservationDao extends JpaRepository<ReservationEntity, UUID> {

  /**
   * Lista reservas con identidad confirmada pertenecientes al local del propietario autenticado.
   *
   * <p>La consulta excluye holds y expiraciones anónimas mediante {@code customerEmail is not null},
   * aplica todos los filtros en base de datos y precarga la franja para que el adaptador REST no
   * dependa de una sesión JPA abierta.
   */
  @Query(
      value =
          """
          select reservation
          from ReservationEntity reservation
          join fetch reservation.timeSlot
          where reservation.venue.ownerUser.id = :ownerUserId
            and reservation.customerEmail is not null
            and (:fromDate is null or reservation.date >= :fromDate)
            and (:toDateExclusive is null or reservation.date < :toDateExclusive)
            and (:timeSlotId is null or reservation.timeSlot.id = :timeSlotId)
            and (:status is null or reservation.status = :status)
            and (
              :userPattern is null
              or lower(reservation.customerName) like :userPattern escape '\\'
              or reservation.customerEmailNormalized like :userPattern escape '\\'
            )
          order by reservation.date desc, reservation.startsAt desc, reservation.createdAt desc
          """,
      countQuery =
          """
          select count(reservation)
          from ReservationEntity reservation
          where reservation.venue.ownerUser.id = :ownerUserId
            and reservation.customerEmail is not null
            and (:fromDate is null or reservation.date >= :fromDate)
            and (:toDateExclusive is null or reservation.date < :toDateExclusive)
            and (:timeSlotId is null or reservation.timeSlot.id = :timeSlotId)
            and (:status is null or reservation.status = :status)
            and (
              :userPattern is null
              or lower(reservation.customerName) like :userPattern escape '\\'
              or reservation.customerEmailNormalized like :userPattern escape '\\'
            )
          """)
  Page<ReservationEntity> findOwnedReservations(
      @Param("ownerUserId") UUID ownerUserId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDateExclusive") LocalDate toDateExclusive,
      @Param("timeSlotId") UUID timeSlotId,
      @Param("status") String status,
      @Param("userPattern") String userPattern,
      Pageable pageable);

  /**
   * Obtiene el detalle exclusivamente a través de la frontera de propiedad del local autenticado.
   * Reserva inexistente y reserva ajena producen la misma ausencia para no revelar identificadores.
   */
  @Query(
      """
      select reservation
      from ReservationEntity reservation
      join fetch reservation.timeSlot
      where reservation.id = :reservationId
        and reservation.venue.ownerUser.id = :ownerUserId
        and reservation.customerEmail is not null
      """)
  Optional<ReservationEntity> findOwnedDetail(
      @Param("ownerUserId") UUID ownerUserId, @Param("reservationId") UUID reservationId);

  /**
   * Bloquea una reserva propia con identidad confirmada antes de cambiar su asistencia.
   *
   * <p>La condición de propietario dentro de la consulta evita leer primero y autorizar después.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select reservation
      from ReservationEntity reservation
      where reservation.id = :reservationId
        and reservation.venue.ownerUser.id = :ownerUserId
        and reservation.customerEmail is not null
      """)
  Optional<ReservationEntity> findOwnedForAttendanceUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("reservationId") UUID reservationId);

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
