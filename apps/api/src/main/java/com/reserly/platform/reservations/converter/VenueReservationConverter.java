package com.reserly.platform.reservations.converter;

import com.reserly.platform.reservations.dto.VenueReservationDetailResponse;
import com.reserly.platform.reservations.dto.VenueReservationListResponse;
import com.reserly.platform.reservations.dto.VenueReservationSummaryResponse;
import com.reserly.platform.reservations.persistence.ReservationEntity;
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

  /** Convierte una reserva propia al detalle básico de la tarea 9.3. */
  public VenueReservationDetailResponse toDetailResponse(ReservationEntity reservation) {
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
        reservation.getUpdatedAt());
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
