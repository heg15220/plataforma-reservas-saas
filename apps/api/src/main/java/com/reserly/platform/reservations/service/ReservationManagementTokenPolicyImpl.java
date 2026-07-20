package com.reserly.platform.reservations.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Mantiene el enlace disponible hasta treinta días después del final de la cita. */
@Service
public class ReservationManagementTokenPolicyImpl implements ReservationManagementTokenPolicy {

  private static final Duration RETENTION_AFTER_APPOINTMENT = Duration.ofDays(30);

  @Override
  public Instant expiresAt(LocalDate date, LocalTime endsAt, ZoneId zoneId) {
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(endsAt, "endsAt");
    Objects.requireNonNull(zoneId, "zoneId");
    return date.atTime(endsAt).atZone(zoneId).toInstant().plus(RETENTION_AFTER_APPOINTMENT);
  }
}
