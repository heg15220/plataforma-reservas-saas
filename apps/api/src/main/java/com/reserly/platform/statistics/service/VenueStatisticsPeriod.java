package com.reserly.platform.statistics.service;

import java.util.Locale;

/** Filtros temporales cerrados admitidos por el panel MVP. */
public enum VenueStatisticsPeriod {
  TODAY,
  WEEK,
  MONTH,
  YEAR,
  CUSTOM;

  /** Convierte el valor HTTP sin depender del locale de la JVM. */
  public static VenueStatisticsPeriod parse(String value) {
    if (value == null || value.isBlank()) {
      return TODAY;
    }
    try {
      return valueOf(value.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new VenueStatisticsFilterInvalidException();
    }
  }

  /** Valor canónico enviado a frontend. */
  public String externalValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
