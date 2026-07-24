package com.reserly.platform.reservations.service;

import java.util.Locale;
import java.util.Optional;

/** Periodos de calendario admitidos por el panel de reservas. */
public enum VenueReservationPeriod {
  DAY,
  WEEK,
  MONTH;

  /** Convierte el valor HTTP sin depender de mayúsculas ni del locale de la JVM. */
  public static Optional<VenueReservationPeriod> parse(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException exception) {
      throw new VenueReservationFilterInvalidException();
    }
  }
}
