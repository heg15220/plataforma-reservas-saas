package com.reserly.platform.demand.attribution.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso idempotente a la clasificación comercial de reservas. */
public interface BookingAttributionDao extends JpaRepository<BookingAttributionEntity, UUID> {

  /** Resuelve reintentos sin recalcular o duplicar la atribución. */
  @Query(
      "select attribution from BookingAttributionEntity attribution"
          + " where attribution.reservation.id = :reservationId")
  Optional<BookingAttributionEntity> findByReservationId(
      @Param("reservationId") UUID reservationId);
}
