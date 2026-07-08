package com.reserly.platform.availability.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de bloqueos de disponibilidad siempre acotada por propietario. */
public interface AvailabilityBlockDao extends JpaRepository<AvailabilityBlockEntity, UUID> {

  /** Busca la excepción de día completo del propietario para una fecha. */
  @Query(
      """
      select block from AvailabilityBlockEntity block
      where block.venue.ownerUser.id = :ownerUserId
        and block.venue.status <> 'archived'
        and block.scope = 'venue'
        and block.date = :date
        and block.startsAt is null
        and block.endsAt is null
        and block.kind in ('closed_day', 'reservations_disabled')
      """)
  Optional<AvailabilityBlockEntity> findOwnedDayOverride(
      @Param("ownerUserId") UUID ownerUserId, @Param("date") LocalDate date);

  /** Bloquea cualquier excepción de día completo existente antes de reemplazarla. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      select block from AvailabilityBlockEntity block
      where block.venue.ownerUser.id = :ownerUserId
        and block.venue.status <> 'archived'
        and block.scope = 'venue'
        and block.date = :date
        and block.startsAt is null
        and block.endsAt is null
        and block.kind in ('closed_day', 'reservations_disabled')
      """)
  List<AvailabilityBlockEntity> findOwnedDayOverridesForUpdate(
      @Param("ownerUserId") UUID ownerUserId, @Param("date") LocalDate date);

  /** Indica si una fecha tiene cierre completo o reservas desactivadas. */
  @Query(
      """
      select count(block) > 0 from AvailabilityBlockEntity block
      where block.venue.ownerUser.id = :ownerUserId
        and block.venue.status <> 'archived'
        and block.scope = 'venue'
        and block.date = :date
        and block.startsAt is null
        and block.endsAt is null
        and block.kind in ('closed_day', 'reservations_disabled')
      """)
  boolean existsOwnedDayOverride(
      @Param("ownerUserId") UUID ownerUserId, @Param("date") LocalDate date);

  /** Busca la excepción de día completo visible para un local publicado. */
  @Query(
      """
      select block from AvailabilityBlockEntity block
      where block.venue.id = :venueId
        and block.venue.status = 'published'
        and block.scope = 'venue'
        and block.date = :date
        and block.startsAt is null
        and block.endsAt is null
        and block.kind in ('closed_day', 'reservations_disabled')
      """)
  Optional<AvailabilityBlockEntity> findPublishedDayOverride(
      @Param("venueId") UUID venueId, @Param("date") LocalDate date);
}
