package com.reserly.platform.reservations.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Detalle privado completo de una reserva del local.
 *
 * <p>Los hashes, secretos de gestión y datos sensibles de incidencias nunca forman parte del
 * contrato.
 */
public record VenueReservationDetailResponse(
    UUID id,
    UUID timeSlotId,
    UUID serviceId,
    String customerName,
    String customerEmail,
    int partySize,
    LocalDate date,
    LocalTime startsAt,
    LocalTime endsAt,
    String status,
    boolean manualActionsAvailable,
    Instant cancelledAt,
    String cancelledBy,
    String cancellationReason,
    Instant createdAt,
    Instant updatedAt,
    List<VenueReservationFormAnswerResponse> formAnswers,
    VenueReservationAssignedResourceResponse assignedResource,
    VenueReservationIncidentHistoryResponse incidentHistory) {

  public VenueReservationDetailResponse {
    formAnswers = List.copyOf(formAnswers);
  }
}
