package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationErrorResponse;
import com.reserly.platform.reservations.service.ReservationCapacityUnavailableException;
import com.reserly.platform.reservations.service.ReservationConfirmationInvalidException;
import com.reserly.platform.reservations.service.ReservationFormAnswersInvalidException;
import com.reserly.platform.reservations.service.ReservationHoldExpiredException;
import org.springframework.http.HttpStatus;
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

  /** Expone un error estable sin revelar claves, etiquetas ni reglas internas del formulario. */
  @ExceptionHandler(ReservationFormAnswersInvalidException.class)
  public ResponseEntity<ReservationErrorResponse> handleInvalidFormAnswers() {
    return ResponseEntity.badRequest()
        .body(new ReservationErrorResponse("RESERVATION_FORM_INVALID"));
  }

  /** El cliente acreditó el hold, por lo que puede recibir una causa recuperable específica. */
  @ExceptionHandler(ReservationHoldExpiredException.class)
  public ResponseEntity<ReservationErrorResponse> handleExpiredHold() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ReservationErrorResponse("RESERVATION_HOLD_EXPIRED"));
  }

  /** La capacidad cambió tras crear el hold y exige reiniciar la selección de franja. */
  @ExceptionHandler(ReservationCapacityUnavailableException.class)
  public ResponseEntity<ReservationErrorResponse> handleUnavailableCapacity() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ReservationErrorResponse("RESERVATION_CAPACITY_UNAVAILABLE"));
  }
}
