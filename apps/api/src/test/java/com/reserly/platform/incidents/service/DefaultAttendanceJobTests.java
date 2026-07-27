package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.persistence.ReservationDao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/** Verifica frontera, zona y frecuencia del job sin arrancar scheduler ni infraestructura. */
class DefaultAttendanceJobTests {

  private static final Instant NOW = Instant.parse("2026-07-27T18:00:00Z");
  private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

  @Test
  void marksOnlyRowsSelectedByAtomicDaoAgainstOneBoundary() {
    ReservationDao reservationDao = mock(ReservationDao.class);
    when(reservationDao.markUnresolvedFinishedReservationsAttended(NOW, ZONE.getId()))
        .thenReturn(4);
    DefaultAttendanceJob job =
        new DefaultAttendanceJob(reservationDao, Clock.fixed(NOW, ZONE));

    int updated = job.markAttendedByDefault();

    assertThat(updated).isEqualTo(4);
    verify(reservationDao)
        .markUnresolvedFinishedReservationsAttended(NOW, "Europe/Madrid");
  }

  @Test
  void declaresBoundedConfigurableSchedule() throws NoSuchMethodException {
    Scheduled scheduled =
        DefaultAttendanceJob.class
            .getMethod("markAttendedByDefault")
            .getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.fixedDelayString())
        .isEqualTo("${reserly.incidents.default-attendance.fixed-delay:5m}");
    assertThat(scheduled.initialDelayString())
        .isEqualTo("${reserly.incidents.default-attendance.initial-delay:1m}");
  }
}
