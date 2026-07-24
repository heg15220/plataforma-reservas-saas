package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationErrorResponse;
import com.reserly.platform.reservations.service.ReservationCancellationNotAllowedException;
import com.reserly.platform.reservations.service.ReservationManagementNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Mantiene errores seguros y distingue solo la política recuperable de un enlace válido. */
@RestControllerAdvice(assignableTypes = ReservationManagementControllerImpl.class)
public class ReservationManagementExceptionHandler {

  @ExceptionHandler(ReservationManagementNotFoundException.class)
  ResponseEntity<ReservationErrorResponse> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ReservationErrorResponse("RESERVATION_MANAGEMENT_LINK_INVALID"));
  }

  @ExceptionHandler(ReservationCancellationNotAllowedException.class)
  ResponseEntity<ReservationErrorResponse> cancellationNotAllowed() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ReservationErrorResponse("RESERVATION_CANCELLATION_DEADLINE_PASSED"));
  }
}
