package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationErrorResponse;
import com.reserly.platform.reservations.service.ReservationManagementNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Produce una respuesta indistinguible para token inválido, caducado o revocado. */
@RestControllerAdvice(assignableTypes = ReservationManagementControllerImpl.class)
public class ReservationManagementExceptionHandler {

  @ExceptionHandler(ReservationManagementNotFoundException.class)
  ResponseEntity<ReservationErrorResponse> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new ReservationErrorResponse("RESERVATION_MANAGEMENT_LINK_INVALID"));
  }
}
