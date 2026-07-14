package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationErrorResponse;
import com.reserly.platform.reservations.service.ReservationConfirmationInvalidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce todos los rechazos de confirmación a un contrato que no permite enumerar reservas. */
@RestControllerAdvice(assignableTypes = ReservationConfirmationControllerImpl.class)
public class ReservationConfirmationExceptionHandler {

  /** Unifica errores estructurales, de propiedad del hold y de transición. */
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    ReservationConfirmationInvalidException.class
  })
  public ResponseEntity<ReservationErrorResponse> handleInvalidConfirmation() {
    return ResponseEntity.badRequest()
        .body(new ReservationErrorResponse("RESERVATION_CONFIRMATION_INVALID"));
  }
}
