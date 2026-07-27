package com.reserly.platform.incidents.service;

import com.reserly.platform.reservations.persistence.ReservationDao;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve periódicamente reservas finalizadas sin decisión de asistencia.
 *
 * <p>La actualización masiva es idempotente: solo toca {@code confirmed} con
 * {@code attendanceMarkedAt IS NULL}. No publica identificadores ni datos de clientes en logs.
 */
@Component
public class DefaultAttendanceJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAttendanceJob.class);

  private final ReservationDao reservationDao;
  private final Clock clock;

  public DefaultAttendanceJob(ReservationDao reservationDao, Clock clock) {
    this.reservationDao = reservationDao;
    this.clock = clock;
  }

  /**
   * Marca como asistidas las reservas cuyo periodo configurable ya terminó.
   *
   * @return número de filas modificadas para métricas operativas
   */
  @Scheduled(
      fixedDelayString = "${reserly.incidents.default-attendance.fixed-delay:5m}",
      initialDelayString = "${reserly.incidents.default-attendance.initial-delay:1m}")
  @Transactional
  public int markAttendedByDefault() {
    Instant now = clock.instant();
    int updated =
        reservationDao.markUnresolvedFinishedReservationsAttended(
            now, clock.getZone().getId());
    if (updated > 0) {
      LOGGER.info("Marked {} finished reservations attended by default at {}", updated, now);
    }
    return updated;
  }
}
