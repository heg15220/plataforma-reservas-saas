package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;

/** Casos de uso de lectura del panel de reservas, siempre acotados por propietario. */
public interface VenueReservationService {

  /**
   * Lista reservas propias aplicando periodos de calendario, franja, estado, usuario y paginación.
   *
   * @throws VenueReservationFilterInvalidException si algún filtro o límite no es válido
   */
  Page<ReservationEntity> list(
      UUID ownerUserId,
      String period,
      LocalDate anchorDate,
      UUID timeSlotId,
      String status,
      String user,
      int page,
      int size);

  /**
   * Recupera una reserva propia o devuelve una ausencia opaca.
   *
   * @throws VenueReservationNotFoundException si no existe, no tiene identidad o pertenece a otro
   *     local
   */
  VenueReservationDetail findDetail(UUID ownerUserId, UUID reservationId);
}
