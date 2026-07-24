package com.reserly.platform.reservations.service;

import com.reserly.platform.reservations.dto.ManagedReservationResponse;
import com.reserly.platform.reservations.dto.ReservationCancellationResponse;

/** Consulta y cancelación seguras de una reserva sin cuenta mediante secreto opaco. */
public interface ReservationManagementService {

  /** Devuelve solo la reserva asociada o un error uniforme para cualquier secreto no utilizable. */
  ManagedReservationResponse findByToken(String token);

  /** Cancela bajo lock si el secreto, estado y plazo siguen siendo válidos. */
  ReservationCancellationResponse cancelByToken(String token);
}
