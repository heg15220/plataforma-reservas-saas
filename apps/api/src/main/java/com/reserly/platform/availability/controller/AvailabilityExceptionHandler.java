package com.reserly.platform.availability.controller;

import com.reserly.platform.availability.dto.AvailabilityErrorResponse;
import com.reserly.platform.availability.service.OpeningHoursInvalidException;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de disponibilidad a códigos estables sin revelar constraints internas. */
@RestControllerAdvice(assignableTypes = {OpeningHoursControllerImpl.class})
public class AvailabilityExceptionHandler {

  @ExceptionHandler({MethodArgumentNotValidException.class, OpeningHoursInvalidException.class})
  public ResponseEntity<AvailabilityErrorResponse> handleInvalidOpeningHours() {
    return ResponseEntity.badRequest().body(new AvailabilityErrorResponse("OPENING_HOURS_INVALID"));
  }

  @ExceptionHandler(VenueProfileNotFoundException.class)
  public ResponseEntity<AvailabilityErrorResponse> handleVenueNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new AvailabilityErrorResponse("VENUE_PROFILE_NOT_FOUND"));
  }
}
