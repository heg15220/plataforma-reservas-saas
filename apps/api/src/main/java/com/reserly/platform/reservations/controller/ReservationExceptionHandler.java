package com.reserly.platform.reservations.controller;

import com.reserly.platform.reservations.dto.ReservationErrorResponse;
import com.reserly.platform.reservations.service.ReservationHoldInvalidException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce rechazos públicos sin exponer existencia de locales, franjas o recursos. */
@RestControllerAdvice(assignableTypes = ReservationHoldControllerImpl.class)
public class ReservationExceptionHandler {

  /** Unifica validación estructural y de negocio bajo un código estable. */
  @ExceptionHandler({MethodArgumentNotValidException.class, ReservationHoldInvalidException.class})
  public ResponseEntity<ReservationErrorResponse> handleInvalidHold() {
    return ResponseEntity.badRequest()
        .body(new ReservationErrorResponse("RESERVATION_HOLD_INVALID"));
  }
}
