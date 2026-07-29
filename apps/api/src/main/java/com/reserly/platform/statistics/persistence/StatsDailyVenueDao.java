package com.reserly.platform.statistics.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistencia de métricas diarias con una agregación PostgreSQL atómica e idempotente. */
public interface StatsDailyVenueDao extends JpaRepository<StatsDailyVenueEntity, UUID> {

  /** Lee una serie diaria propia ya ordenada y sin cargar datos transaccionales de clientes. */
  @Query(
      """
      select stats
      from StatsDailyVenueEntity stats
      where stats.venue.id = :venueId
        and stats.date between :fromDate and :toDate
      order by stats.date
      """)
  List<StatsDailyVenueEntity> findRange(
      @Param("venueId") UUID venueId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);

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

  /**
   * Recalcula en una sola sentencia un rango inclusivo de un único local autenticado.
   *
   * <p>{@code generate_series} produce también días sin actividad. La zona IANA convierte los
   * instantes de reseñas a su fecha local; reservas y franjas ya guardan una fecha snapshot.
   *
   * @return número de días insertados o actualizados
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value =
          """
          WITH dates AS (
            SELECT CAST(
              generate_series(
                CAST(:fromDate AS date),
                CAST(:toDate AS date),
                INTERVAL '1 day'
              )
              AS date
            ) AS "date"
          ),
          reservationStats AS (
            SELECT
              reservation."date" AS "date",
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
            WHERE reservation."venueId" = :venueId
              AND reservation."date" BETWEEN :fromDate AND :toDate
              AND reservation."customerEmailNormalized" IS NOT NULL
              AND reservation."status" IN (
                'confirmed', 'attended', 'no_show', 'reported',
                'cancelled_by_user', 'cancelled_by_venue'
              )
            GROUP BY reservation."date"
          ),
          capacityStats AS (
            SELECT
              slot."date" AS "date",
              COALESCE(SUM(slot."capacity"), 0) AS "availableCapacity"
            FROM "TimeSlots" slot
            WHERE slot."venueId" = :venueId
              AND slot."date" BETWEEN :fromDate AND :toDate
              AND slot."status" IN ('available', 'full')
            GROUP BY slot."date"
          ),
          reviewStats AS (
            SELECT
              CAST(review."createdAt" AT TIME ZONE :zoneId AS date) AS "date",
              COUNT(*) AS "reviewsCount",
              ROUND(AVG(review."rating"), 2) AS "averageRating"
            FROM "Reviews" review
            WHERE review."venueId" = :venueId
              AND CAST(review."createdAt" AT TIME ZONE :zoneId AS date)
                BETWEEN :fromDate AND :toDate
            GROUP BY CAST(review."createdAt" AT TIME ZONE :zoneId AS date)
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
            :venueId,
            dates."date",
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
          FROM dates
          LEFT JOIN reservationStats ON reservationStats."date" = dates."date"
          LEFT JOIN capacityStats ON capacityStats."date" = dates."date"
          LEFT JOIN reviewStats ON reviewStats."date" = dates."date"
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
  int aggregateVenueRange(
      @Param("venueId") UUID venueId,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate,
      @Param("zoneId") String zoneId,
      @Param("calculatedAt") Instant calculatedAt);
}
