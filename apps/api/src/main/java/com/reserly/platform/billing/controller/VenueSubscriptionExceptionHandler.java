package com.reserly.platform.billing.controller;

import com.reserly.platform.billing.dto.BillingErrorResponse;
import com.reserly.platform.billing.service.VenueSubscriptionNotFoundException;
import com.reserly.platform.billing.service.VenueSubscriptionUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce fallos de catálogo y propiedad sin exponer IDs ni configuración interna. */
@RestControllerAdvice(assignableTypes = VenueSubscriptionControllerImpl.class)
public class VenueSubscriptionExceptionHandler {

  @ExceptionHandler(VenueSubscriptionNotFoundException.class)
  ResponseEntity<BillingErrorResponse> notFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new BillingErrorResponse("VENUE_SUBSCRIPTION_NOT_FOUND"));
  }

  @ExceptionHandler(VenueSubscriptionUnavailableException.class)
  ResponseEntity<BillingErrorResponse> unavailable() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new BillingErrorResponse("VENUE_SUBSCRIPTION_UNAVAILABLE"));
  }
}
