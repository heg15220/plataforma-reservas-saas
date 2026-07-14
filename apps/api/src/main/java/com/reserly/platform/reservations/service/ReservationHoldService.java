package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;

/** Caso de uso público que crea el estado inicial de una reserva sin datos personales. */
public interface ReservationHoldService {

  /**
   * Valida la selección, asigna recurso cuando procede y persiste el token solo como hash.
   *
   * @throws ReservationHoldInvalidException si la selección no es reservable
   */
  ReservationHoldResponse create(ReservationHoldRequest request);
}
