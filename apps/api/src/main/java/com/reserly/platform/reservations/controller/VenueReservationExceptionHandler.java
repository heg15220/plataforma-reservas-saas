package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.VenueReservationErrorResponse;
import com.reserly.platform.reservations.service.VenueReservationCancellationInvalidException;
import com.reserly.platform.reservations.service.VenueReservationCancellationNotFoundException;
import com.reserly.platform.reservations.service.VenueReservationFilterInvalidException;
import com.reserly.platform.reservations.service.VenueReservationNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores del panel a códigos estables y sin información sobre reservas ajenas. */
@RestControllerAdvice(
    assignableTypes = {
      VenueReservationControllerImpl.class,
      VenueReservationCancellationControllerImpl.class
    })
public class VenueReservationExceptionHandler {

  /** Un filtro inválido es un error de contrato y no llega a persistencia. */
  @ExceptionHandler(VenueReservationFilterInvalidException.class)
  ResponseEntity<VenueReservationErrorResponse> handleInvalidFilter() {
    return ResponseEntity.badRequest()
        .body(new VenueReservationErrorResponse("VENUE_RESERVATION_FILTER_INVALID"));
  }

  /** Reserva inexistente y ajena comparten respuesta para impedir enumeración de UUID. */
  @ExceptionHandler(VenueReservationNotFoundException.class)
  ResponseEntity<VenueReservationErrorResponse> handleNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new VenueReservationErrorResponse("VENUE_RESERVATION_NOT_FOUND"));
  }

  /** Rechaza motivos ausentes o fuera de límite como error de contrato. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<VenueReservationErrorResponse> handleInvalidCancellationRequest() {
    return ResponseEntity.badRequest()
        .body(new VenueReservationErrorResponse("VENUE_RESERVATION_CANCELLATION_INVALID"));
  }

  /** Una reserva existente pero no cancelable produce conflicto sin revelar su estado. */
  @ExceptionHandler(VenueReservationCancellationInvalidException.class)
  ResponseEntity<VenueReservationErrorResponse> handleInvalidCancellationTransition() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new VenueReservationErrorResponse("VENUE_RESERVATION_CANCELLATION_INVALID"));
  }

  @ExceptionHandler(VenueReservationCancellationNotFoundException.class)
  ResponseEntity<VenueReservationErrorResponse> handleCancellationNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new VenueReservationErrorResponse("VENUE_RESERVATION_NOT_FOUND"));
  }
}
