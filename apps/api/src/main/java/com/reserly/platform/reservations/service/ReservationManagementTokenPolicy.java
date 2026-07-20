package com.reserly.platform.reservations.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/** Define la caducidad absoluta del secreto de gestión de una reserva confirmada. */
public interface ReservationManagementTokenPolicy {

  /** Devuelve una caducidad posterior a la cita usando la zona operativa del reloj del servicio. */
  Instant expiresAt(LocalDate date, LocalTime endsAt, ZoneId zoneId);
}
