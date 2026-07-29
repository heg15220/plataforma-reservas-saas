package com.reserly.platform.statistics.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de métricas diarias con una agregación PostgreSQL atómica e idempotente. */
public interface StatsDailyVenueDao extends JpaRepository<StatsDailyVenueEntity, UUID> {

  /**
   * Regenera una fecha para todos los locales desde reservas, franjas y reseñas.
   *
   * <p>Una reserva contabilizable ya recopiló identidad y conserva uno de los estados posteriores a
   * la confirmación. Las canceladas forman parte del total, pero no de la ocupación. {@code reported}
   * sigue siendo una no asistencia y evita perder el dato al evolucionar el workflow.
   *
   * @param statsDate fecha local agregada
   * @param dayStart inicio inclusivo de la fecha en la zona del negocio
   * @param dayEnd final exclusivo de la fecha en la zona del negocio
   * @param calculatedAt instante común de actualización
   * @return número de locales insertados o actualizados
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          WITH reservationStats AS (
            SELECT
              reservation."venueId" AS "venueId",
              COUNT(*) AS "reservationsCount",
              COUNT(*) FILTER (
                WHERE reservation."status" IN (
                  'confirmed', 'attended', 'no_show', 'reported'
                )
              ) AS "confirmedCount",
              COUNT(*) FILTER (
                WHERE reservation."status" IN (
                  'cancelled_by_user', 'cancelled_by_venue'
                )
              ) AS "cancelledCount",
              COUNT(*) FILTER (
                WHERE reservation."status" IN ('no_show', 'reported')
              ) AS "noShowCount",
              COUNT(*) FILTER (
                WHERE reservation."status" = 'attended'
              ) AS "attendedCount",
              COALESCE(
                SUM(reservation."partySize") FILTER (
                  WHERE reservation."status" IN (
                    'confirmed', 'attended', 'no_show', 'reported'
                  )
                ),
                0
              ) AS "occupiedCapacity"
            FROM "Reservations" reservation
            WHERE reservation."date" = :statsDate
              AND reservation."customerEmailNormalized" IS NOT NULL
              AND reservation."status" IN (
                'confirmed', 'attended', 'no_show', 'reported',
                'cancelled_by_user', 'cancelled_by_venue'
              )
            GROUP BY reservation."venueId"
          ),
          capacityStats AS (
            SELECT
              slot."venueId" AS "venueId",
              COALESCE(SUM(slot."capacity"), 0) AS "availableCapacity"
            FROM "TimeSlots" slot
            WHERE slot."date" = :statsDate
              AND slot."status" IN ('available', 'full')
            GROUP BY slot."venueId"
          ),
          reviewStats AS (
            SELECT
              review."venueId" AS "venueId",
              COUNT(*) AS "reviewsCount",
              ROUND(AVG(review."rating"), 2) AS "averageRating"
            FROM "Reviews" review
            WHERE review."createdAt" >= :dayStart
              AND review."createdAt" < :dayEnd
            GROUP BY review."venueId"
          )
          INSERT INTO "StatsDailyVenue" (
            "id",
            "venueId",
            "date",
            "reservationsCount",
            "confirmedCount",
            "cancelledCount",
            "noShowCount",
            "attendedCount",
            "occupiedCapacity",
            "availableCapacity",
            "reviewsCount",
            "averageRating",
            "createdAt",
            "updatedAt"
          )
          SELECT
            gen_random_uuid(),
            venue."id",
            :statsDate,
            COALESCE(reservationStats."reservationsCount", 0),
            COALESCE(reservationStats."confirmedCount", 0),
            COALESCE(reservationStats."cancelledCount", 0),
            COALESCE(reservationStats."noShowCount", 0),
            COALESCE(reservationStats."attendedCount", 0),
            COALESCE(reservationStats."occupiedCapacity", 0),
            COALESCE(capacityStats."availableCapacity", 0),
            COALESCE(reviewStats."reviewsCount", 0),
            reviewStats."averageRating",
            :calculatedAt,
            :calculatedAt
          FROM "Venues" venue
          LEFT JOIN reservationStats ON reservationStats."venueId" = venue."id"
          LEFT JOIN capacityStats ON capacityStats."venueId" = venue."id"
          LEFT JOIN reviewStats ON reviewStats."venueId" = venue."id"
          ON CONFLICT ("venueId", "date") DO UPDATE SET
            "reservationsCount" = EXCLUDED."reservationsCount",
            "confirmedCount" = EXCLUDED."confirmedCount",
            "cancelledCount" = EXCLUDED."cancelledCount",
            "noShowCount" = EXCLUDED."noShowCount",
            "attendedCount" = EXCLUDED."attendedCount",
            "occupiedCapacity" = EXCLUDED."occupiedCapacity",
            "availableCapacity" = EXCLUDED."availableCapacity",
            "reviewsCount" = EXCLUDED."reviewsCount",
            "averageRating" = EXCLUDED."averageRating",
            "updatedAt" = EXCLUDED."updatedAt"
          """,
      nativeQuery = true)
  int aggregateDate(
      @Param("statsDate") LocalDate statsDate,
      @Param("dayStart") Instant dayStart,
      @Param("dayEnd") Instant dayEnd,
      @Param("calculatedAt") Instant calculatedAt);
}
