package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.persistence.ReservationDao;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Libera periódicamente la capacidad retenida por holds vencidos.
 *
 * <p>Cada ejecución usa una sola frontera temporal UTC y una actualización masiva condicional. El
 * trabajo es idempotente y seguro ante ejecuciones repetidas o nodos concurrentes: el DAO solo
 * modifica filas que continúan en estado {@code hold} al adquirir el bloqueo de escritura.
 */
@Component
public class ReservationHoldExpirationJob {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ReservationHoldExpirationJob.class);

  private final ReservationDao reservationDao;
  private final Clock clock;

  /** Crea el job productivo con un reloj UTC del sistema. */
  @Autowired
  public ReservationHoldExpirationJob(ReservationDao reservationDao) {
    this(reservationDao, Clock.systemUTC());
  }

  ReservationHoldExpirationJob(ReservationDao reservationDao, Clock clock) {
    this.reservationDao = reservationDao;
    this.clock = clock;
  }

  /**
   * Cambia a {@code expired} todos los holds vencidos antes del inicio de este ciclo.
   *
   * @return cantidad de reservas expiradas; se expone para verificación y observabilidad local
   */
  @Scheduled(
      fixedDelayString = "${reserly.reservations.holdExpiration.fixedDelay:1m}",
      initialDelayString = "${reserly.reservations.holdExpiration.initialDelay:1m}")
  @Transactional
  public int expireOverdueHolds() {
    Instant now = clock.instant();
    int expiredCount = reservationDao.expireHoldsBefore(now);
    if (expiredCount > 0) {
      LOGGER.info("Expired {} overdue reservation holds at {}", expiredCount, now);
    }
    return expiredCount;
  }
}