package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.util.Map;
import org.springframework.data.domain.Page;

/**
 * Página privada junto con niveles de incidencia minimizados, indexados solo durante la conversión.
 */
public record VenueReservationPage(
    Page<ReservationEntity> reservations,
    Map<String, VenueReservationIncidentRisk> incidentRiskByCustomerEmail) {

  public VenueReservationPage {
    incidentRiskByCustomerEmail = Map.copyOf(incidentRiskByCustomerEmail);
  }

  /** Devuelve nivel bajo cuando el email no tiene ninguna incidencia operativa agregada. */
  public VenueReservationIncidentRisk incidentRiskFor(ReservationEntity reservation) {
    return incidentRiskByCustomerEmail.getOrDefault(
        reservation.getCustomerEmailNormalized(), VenueReservationIncidentRisk.LOW);
  }
}
