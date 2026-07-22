package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.dto.ManagedReservationResponse;

/** Consulta segura de una reserva sin cuenta mediante un secreto opaco. */
public interface ReservationManagementService {

  /** Devuelve solo la reserva asociada o un error uniforme para cualquier secreto no utilizable. */
  ManagedReservationResponse findByToken(String token);
}
