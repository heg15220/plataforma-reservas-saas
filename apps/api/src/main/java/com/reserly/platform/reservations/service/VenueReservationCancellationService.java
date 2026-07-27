package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.dto.VenueReservationCancellationRequest;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.util.UUID;

/** Caso de uso transaccional de cancelación preventiva iniciada por un propietario. */
public interface VenueReservationCancellationService {

  /**
   * Cancela una reserva futura propia, audita el motivo y solicita la notificación post-commit.
   */
  ReservationEntity cancel(
      UUID ownerUserId,
      UUID reservationId,
      VenueReservationCancellationRequest request,
      VenueReservationCancellationAuditContext auditContext);
}
