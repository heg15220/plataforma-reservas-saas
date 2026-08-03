package com.reserly.platform.reservations.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Ejecuta la consulta privada real sobre PostgreSQL para proteger el tipado de filtros opcionales.
 */
@SpringBootTest
@ActiveProfiles("test")
class VenueReservationQueryIntegrationTests {

  @Autowired private ReservationDao reservationDao;
  @Autowired private NoShowIncidentDao incidentDao;

  @Test
  void executesDailyAgendaWithNullOptionalFiltersWithoutPostgresTypeErrors() {
    LocalDate date = LocalDate.of(2026, 8, 3);

    assertThat(
            reservationDao.findAccessibleReservations(
                UUID.randomUUID(), date, date.plusDays(1), null, null, "", PageRequest.of(0, 25)))
        .isEmpty();
  }

  @Test
  void executesThePagedIncidentRiskAggregateWithoutExposingHistory() {
    Instant now = Instant.parse("2026-08-03T12:00:00Z");

    assertThat(
            incidentDao.summarizeOperationalRisk(
                Set.of("absent-risk-fixture@example.test"),
                now.minus(365, ChronoUnit.DAYS),
                now.minus(180, ChronoUnit.DAYS)))
        .isEmpty();
  }
}
