package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.dto.ReservationConfirmRequest;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import java.util.UUID;

/** Caso de uso transaccional para confirmar el hold que posee el cliente anónimo. */
public interface ReservationConfirmationService {

  /**
   * Confirma una reserva identificada por su secreto de hold de una sola exposición.
   *
   * @throws ReservationConfirmationInvalidException si el agregado, el secreto, los consentimientos
   *     o los datos mínimos no permiten la transición
   * @throws ReservationHoldExpiredException si el token acredita el hold pero su límite temporal
   *     exclusivo ya venció
   * @throws ReservationCapacityUnavailableException si la capacidad real bajo lock es insuficiente
   */
  ReservationConfirmResponse confirm(UUID reservationId, ReservationConfirmRequest request);
}
