package com.reserly.platform.demand.waitlist.controller;

import com.reserly.platform.demand.waitlist.service.WaitlistOfferUnavailableException;
import com.reserly.platform.reservations.dto.ReservationErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Unifica los rechazos sin revelar existencia, estado, caducidad o capacidad de una oferta. */
@RestControllerAdvice(assignableTypes = WaitlistOfferAcceptanceControllerImpl.class)
public class WaitlistOfferAcceptanceExceptionHandler {

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    WaitlistOfferUnavailableException.class
  })
  public ResponseEntity<ReservationErrorResponse> handleUnavailableOffer() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ReservationErrorResponse("WAITLIST_OFFER_UNAVAILABLE"));
  }
}
