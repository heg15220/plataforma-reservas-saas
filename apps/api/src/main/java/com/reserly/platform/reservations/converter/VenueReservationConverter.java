package com.reserly.platform.reservations.converter;

import com.reserly.platform.reservations.dto.VenueReservationAssignedResourceResponse;
import com.reserly.platform.reservations.dto.VenueReservationDetailResponse;
import com.reserly.platform.reservations.dto.VenueReservationFormAnswerResponse;
import com.reserly.platform.reservations.dto.VenueReservationIncidentHistoryResponse;
import com.reserly.platform.reservations.dto.VenueReservationIncidentResponse;
import com.reserly.platform.reservations.dto.VenueReservationListResponse;
import com.reserly.platform.reservations.dto.VenueReservationSummaryResponse;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.service.ReservationOperationalWindow;
import com.reserly.platform.reservations.service.VenueReservationDetail;
import com.reserly.platform.reservations.service.VenueReservationPage;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import java.util.List;
import org.springframework.stereotype.Component;

/** Convierte el agregado interno a contratos privados sin exponer hashes ni secretos. */
@Component
public class VenueReservationConverter {

  private final ReservationOperationalWindow operationalWindow;

  public VenueReservationConverter(ReservationOperationalWindow operationalWindow) {
    this.operationalWindow = operationalWindow;
  }

  /** Convierte una página conservando sus metadatos y el orden fijado por persistencia. */
  public VenueReservationListResponse toListResponse(VenueReservationPage page) {
    List<VenueReservationSummaryResponse> items =
        page.reservations().getContent().stream()
            .map(reservation -> toSummaryResponse(reservation, page))
            .toList();
    return new VenueReservationListResponse(
        items,
        page.reservations().getNumber(),
        page.reservations().getSize(),
        page.reservations().getTotalElements(),
        page.reservations().getTotalPages());
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
        operationalWindow.visibleStatus(reservation),
        operationalWindow.allowsManualAction(reservation),
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

  private VenueReservationSummaryResponse toSummaryResponse(
      ReservationEntity reservation, VenueReservationPage page) {
    return new VenueReservationSummaryResponse(
        reservation.getId(),
        reservation.getTimeSlot().getId(),
        reservation.getCustomerName(),
        reservation.getCustomerEmail(),
        reservation.getPartySize(),
        reservation.getDate(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        operationalWindow.visibleStatus(reservation),
        operationalWindow.allowsManualAction(reservation),
        page.incidentRiskFor(reservation).apiValue(),
        reservation.getCreatedAt());
  }
}
