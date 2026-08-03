package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Fuente única para el estado temporal mostrado en agenda y la ventana de acciones manuales.
 *
 * <p>Las fechas y horas snapshot de una reserva se interpretan en la zona configurada por el reloj
 * de negocio. La ventana es semiabierta: desde el inicio, incluido, hasta una hora después,
 * excluida.
 */
@Component
public class ReservationOperationalWindow {

  public static final Duration MANUAL_ACTION_DURATION = Duration.ofHours(1);

  private final Clock clock;

  public ReservationOperationalWindow(Clock clock) {
    this.clock = clock;
  }

  /** Devuelve {@code pending} antes del inicio sin mutar el estado persistido confirmado. */
  public String visibleStatus(ReservationEntity reservation) {
    if ("confirmed".equals(reservation.getStatus())
        && clock.instant().isBefore(start(reservation))) {
      return "pending";
    }
    return reservation.getStatus();
  }

  /** Indica si el local puede decidir asistencia o cancelar en este instante. */
  public boolean allowsManualAction(ReservationEntity reservation) {
    if (!"confirmed".equals(reservation.getStatus())) {
      return false;
    }
    Instant now = clock.instant();
    Instant startsAt = start(reservation);
    return !now.isBefore(startsAt) && now.isBefore(startsAt.plus(MANUAL_ACTION_DURATION));
  }

  private Instant start(ReservationEntity reservation) {
    return reservation
        .getDate()
        .atTime(reservation.getStartsAt())
        .atZone(clock.getZone())
        .toInstant();
  }
}
