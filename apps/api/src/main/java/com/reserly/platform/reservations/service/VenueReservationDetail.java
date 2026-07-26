package com.reserly.platform.reservations.service;

import com.reserly.platform.forms.persistence.ReservationFormResponseEntity;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import java.util.List;

/**
 * Resultado interno del detalle privado tras acreditar propiedad y cargar referencias históricas.
 *
 * @param reservation agregado principal propio
 * @param formResponses snapshots del formulario
 * @param assignedResource recurso histórico asignado o {@code null}
 * @param incidentTotal total global asociado al email normalizado
 * @param incidents tramo reciente y minimizado por el conversor
 */
public record VenueReservationDetail(
    ReservationEntity reservation,
    List<ReservationFormResponseEntity> formResponses,
    EmployeeResourceEntity assignedResource,
    long incidentTotal,
    List<NoShowIncidentEntity> incidents) {

  public VenueReservationDetail {
    formResponses = List.copyOf(formResponses);
    incidents = List.copyOf(incidents);
  }
}
