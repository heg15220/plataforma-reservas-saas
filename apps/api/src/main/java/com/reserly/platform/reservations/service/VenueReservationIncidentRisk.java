package com.reserly.platform.reservations.service;

/** Nivel informativo derivado del historial operativo visible de un cliente. */
public enum VenueReservationIncidentRisk {
  LOW("low"),
  WATCH("watch"),
  HIGH("high");

  private final String apiValue;

  VenueReservationIncidentRisk(String apiValue) {
    this.apiValue = apiValue;
  }

  /** Clasifica con los mismos umbrales documentados para el detalle privado. */
  public static VenueReservationIncidentRisk from(long operationalCount, long recentCount) {
    if (recentCount >= 2 || operationalCount >= 3) {
      return HIGH;
    }
    if (recentCount >= 1 || operationalCount >= 2) {
      return WATCH;
    }
    return LOW;
  }

  public String apiValue() {
    return apiValue;
  }
}
