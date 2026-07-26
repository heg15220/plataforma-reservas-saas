package com.reserly.platform.reservations.dto;

import java.util.List;

/**
 * Historial profesional acotado asociado al email confirmado de la reserva.
 *
 * @param totalElements número total de incidencias conocidas
 * @param truncated indica que existen más elementos que los incluidos
 * @param items tramo reciente, con máximo de 50
 */
public record VenueReservationIncidentHistoryResponse(
    long totalElements, boolean truncated, List<VenueReservationIncidentResponse> items) {

  public VenueReservationIncidentHistoryResponse {
    items = List.copyOf(items);
  }
}
