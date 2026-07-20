package com.reserly.platform.reservations.service;

/** Rechaza respuestas que no cumplen el formulario publicado sin filtrar su esquema interno. */
public class ReservationFormAnswersInvalidException extends RuntimeException {

  public ReservationFormAnswersInvalidException(Throwable cause) {
    super("Las respuestas del formulario de reserva no son válidas.", cause);
  }
}
