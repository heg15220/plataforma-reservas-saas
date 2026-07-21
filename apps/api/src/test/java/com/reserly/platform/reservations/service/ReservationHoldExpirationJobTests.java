package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.persistence.ReservationDao;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

/** Verifica la frontera temporal y el contrato programado sin arrancar infraestructura. */
class ReservationHoldExpirationJobTests {

  private static final Instant NOW = Instant.parse("2026-07-21T10:15:30Z");

  @Test
  void expiresAgainstOneUtcBoundaryAndReportsAffectedRows() {
    ReservationDao reservationDao = mock(ReservationDao.class);
    when(reservationDao.expireHoldsBefore(NOW)).thenReturn(3);
    var job =
        new ReservationHoldExpirationJob(
            reservationDao, Clock.fixed(NOW, ZoneOffset.UTC));

    int expiredCount = job.expireOverdueHolds();

    assertThat(expiredCount).isEqualTo(3);
    verify(reservationDao).expireHoldsBefore(NOW);
  }

  @Test
  void declaresBoundedConfigurableSchedule() throws NoSuchMethodException {
    Scheduled scheduled =
        ReservationHoldExpirationJob.class
            .getMethod("expireOverdueHolds")
            .getAnnotation(Scheduled.class);

    assertThat(scheduled).isNotNull();
    assertThat(scheduled.fixedDelayString())
        .isEqualTo("${reserly.reservations.holdExpiration.fixedDelay:1m}");
    assertThat(scheduled.initialDelayString())
        .isEqualTo("${reserly.reservations.holdExpiration.initialDelay:1m}");
  }
}