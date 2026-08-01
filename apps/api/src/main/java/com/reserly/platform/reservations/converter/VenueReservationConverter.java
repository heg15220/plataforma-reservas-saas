package com.reserly.platform.reservations.converter;

import com.reserly.platform.reservations.dto.VenueReservationAssignedResourceResponse;
import com.reserly.platform.reservations.dto.VenueReservationDetailResponse;
import com.reserly.platform.reservations.dto.VenueReservationFormAnswerResponse;
import com.reserly.platform.reservations.dto.VenueReservationIncidentHistoryResponse;
import com.reserly.platform.reservations.dto.VenueReservationIncidentResponse;
import com.reserly.platform.reservations.dto.VenueReservationListResponse;
import com.reserly.platform.reservations.dto.VenueReservationSummaryResponse;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.service.VenueReservationDetail;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/** Convierte el agregado interno a contratos privados sin exponer hashes ni secretos. */
@Component
public class VenueReservationConverter {

  /** Convierte una página conservando sus metadatos y el orden fijado por persistencia. */
  public VenueReservationListResponse toListResponse(Page<ReservationEntity> reservations) {
    List<VenueReservationSummaryResponse> items =
        reservations.getContent().stream().map(this::toSummaryResponse).toList();
    return new VenueReservationListResponse(
        items,
        reservations.getNumber(),
        reservations.getSize(),
        reservations.getTotalElements(),
        reservations.getTotalPages());
  }

  /** Convierte el detalle acreditado y minimiza respuestas, recurso e historial profesional. */
  public VenueReservationDetailResponse toDetailResponse(VenueReservationDetail detail) {
    ReservationEntity reservation = detail.reservation();
    List<VenueReservationFormAnswerResponse> formAnswers =
        detail.formResponses().stream()
            .map(
                response ->
                    new VenueReservationFormAnswerResponse(
                        response.getFieldKey(),
                        response.getFieldLabel(),
                        response.getValue(),
                        response.getCreatedAt()))
            .toList();
    List<VenueReservationIncidentResponse> incidents =
        detail.incidents().stream()
            .map(
                incident ->
                    new VenueReservationIncidentResponse(
                        incident.getIncidentType(), incident.getReportedAt(), incident.getStatus()))
            .toList();
    return new VenueReservationDetailResponse(
        reservation.getId(),
        reservation.getTimeSlot().getId(),
        reservation.getServiceId(),
        reservation.getCustomerName(),
        reservation.getCustomerEmail(),
        reservation.getPartySize(),
        reservation.getDate(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getStatus(),
        reservation.getCancelledAt(),
        reservation.getCancelledBy(),
        reservation.getCancellationReason(),
        reservation.getCreatedAt(),
        reservation.getUpdatedAt(),
        formAnswers,
        toAssignedResource(detail.assignedResource()),
        new VenueReservationIncidentHistoryResponse(
            detail.incidentTotal(), detail.incidentTotal() > incidents.size(), incidents));
  }

  private VenueReservationAssignedResourceResponse toAssignedResource(
      EmployeeResourceEntity resource) {
    if (resource == null) {
      return null;
    }
    return new VenueReservationAssignedResourceResponse(
        resource.getId(),
        resource.getType(),
        resource.getFirstName(),
        resource.getLastName(),
        resource.getPublicAlias(),
        resource.getSpecialty(),
        resource.getStatus());
  }

  private VenueReservationSummaryResponse toSummaryResponse(ReservationEntity reservation) {
    return new VenueReservationSummaryResponse(
        reservation.getId(),
        reservation.getTimeSlot().getId(),
        reservation.getCustomerName(),
        reservation.getCustomerEmail(),
        reservation.getPartySize(),
        reservation.getDate(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getStatus(),
        reservation.getCreatedAt());
  }
}
