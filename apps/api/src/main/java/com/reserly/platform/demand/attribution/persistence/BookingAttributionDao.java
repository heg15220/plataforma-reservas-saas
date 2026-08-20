package com.reserly.platform.demand.attribution.persistence;

import java.time.LocalDate;
import java.util.List;
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

  /**
   * Agrega en base y aplica la definición v1 de valle sobre la fecha/hora local de la reserva. La
   * respuesta no contiene filas individuales.
   */
  @Query(
      value =
          """
          SELECT
            a."attributionClass" AS "attributionClass",
            a."attributedCurrency" AS "currency",
            COUNT(*) AS "reservationsCount",
            COUNT(*) FILTER (WHERE a."isNewCustomer") AS "newCustomersCount",
            COUNT(*) FILTER (
              WHERE a."attributionClass" <> 'direct'
                AND EXTRACT(ISODOW FROM r."date") BETWEEN 1 AND 5
                AND r."startsAt" >= TIME '14:00'
                AND r."startsAt" < TIME '18:00'
            ) AS "offPeakCoveredCount",
            SUM(a."attributedAmount") AS "attributedIncome"
          FROM "BookingAttributions" a
          JOIN "Reservations" r ON r."id" = a."reservationId"
          WHERE a."venueId" = :venueId
            AND r."date" BETWEEN :fromDate AND :toDate
          GROUP BY a."attributionClass", a."attributedCurrency"
          """,
      nativeQuery = true)
  List<BookingAttributionAggregate> aggregatePeriod(
      @Param("venueId") UUID venueId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);
}
