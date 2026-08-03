package com.reserly.platform.statistics.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Ejecuta la agregación bajo PostgreSQL real, incluida la agrupación local de reseñas. */
@SpringBootTest
@ActiveProfiles("test")
class VenueStatisticsAggregationIntegrationTests {

  private static final UUID CLINIC_ID = UUID.fromString("e3000000-0000-4000-8000-000000000001");

  @Autowired private DataSource dataSource;

  @Autowired private StatsDailyVenueDao statsDao;

  @Autowired private JdbcTemplate jdbcTemplate;

  /**
   * Protege el enlace de {@code zoneId}: PostgreSQL debe agrupar la fecha proyectada sin comparar
   * dos placeholders distintos generados por Hibernate para la misma zona horaria.
   */
  @Test
  @Transactional
  void aggregatesARequestedVenueRangeWithItsLocalReviewDate() {
    new ResourceDatabasePopulator(new ClassPathResource("dev-fixtures/local-demo-venues.sql"))
        .execute(dataSource);
    insertOperationalIncident();
    LocalDate fromDate = LocalDate.of(2026, 8, 1);
    LocalDate toDate = LocalDate.of(2026, 8, 3);

    int aggregated =
        statsDao.aggregateVenueRange(
            CLINIC_ID, fromDate, toDate, "Europe/Madrid", Instant.parse("2026-08-03T18:00:00Z"));

    assertThat(aggregated).isEqualTo(3);
    assertThat(statsDao.findRange(CLINIC_ID, fromDate, toDate))
        .extracting(StatsDailyVenueEntity::getDate)
        .containsExactly(fromDate, fromDate.plusDays(1), toDate);
    assertThat(statsDao.findRange(CLINIC_ID, fromDate, toDate))
        .filteredOn(day -> day.getDate().equals(LocalDate.of(2026, 8, 2)))
        .extracting(StatsDailyVenueEntity::getIncidentsCount)
        .containsExactly(1L);
  }

  /** Inserta una incidencia mínima sin reutilizar datos personales reales del fixture. */
  private void insertOperationalIncident() {
    UUID reservationId = UUID.fromString("e8000000-0000-4000-8000-000000000001");
    jdbcTemplate.update(
        """
        INSERT INTO "Reservations" (
          "id", "venueId", "timeSlotId", "customerName", "customerEmail",
          "customerEmailNormalized", "partySize", "date", "startsAt", "endsAt", "status"
        )
        SELECT
          ?, venue."id", slot."id", 'Paciente de prueba', 'incident@example.invalid',
          'incident@example.invalid', 1, DATE '2026-08-02', TIME '10:00', TIME '10:30', 'reported'
        FROM "Venues" venue
        JOIN LATERAL (
          SELECT candidate."id"
          FROM "TimeSlots" candidate
          WHERE candidate."venueId" = venue."id"
          ORDER BY candidate."date", candidate."startsAt"
          LIMIT 1
        ) slot ON true
        WHERE venue."id" = ?
        """,
        reservationId,
        CLINIC_ID);
    jdbcTemplate.update(
        """
        INSERT INTO "NoShowIncidents" (
          "id", "venueId", "reservationId", "customerEmailNormalized", "incidentType",
          "reportedByUserId", "reportedAt", "status", "createdAt"
        )
        SELECT
          'e9000000-0000-4000-8000-000000000001', venue."id", ?,
          'incident@example.invalid', 'no_show', venue."ownerUserId",
          TIMESTAMPTZ '2026-08-02 10:00:00+00', 'reported',
          TIMESTAMPTZ '2026-08-02 10:00:00+00'
        FROM "Venues" venue
        WHERE venue."id" = ?
        """,
        reservationId,
        CLINIC_ID);
  }
}
