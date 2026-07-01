package com.reserly.platform.venues.controller;

import com.reserly.platform.venues.dto.VenueDescriptionLimitErrorResponse;
import com.reserly.platform.venues.dto.VenueProfileErrorResponse;
import com.reserly.platform.venues.service.VenueDescriptionTooLongException;
import com.reserly.platform.venues.service.VenueProfileConflictException;
import com.reserly.platform.venues.service.VenueProfileForbiddenException;
import com.reserly.platform.venues.service.VenueProfileInvalidException;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores del perfil a códigos estables sin publicar IDs ni constraints. */
@RestControllerAdvice(assignableTypes = VenueProfileControllerImpl.class)
public class VenueProfileExceptionHandler {

  @ExceptionHandler(VenueDescriptionTooLongException.class)
  public ResponseEntity<VenueDescriptionLimitErrorResponse> handleDescriptionTooLong(
      VenueDescriptionTooLongException exception) {
    return ResponseEntity.status(422)
        .body(
            new VenueDescriptionLimitErrorResponse(
                "VENUE_DESCRIPTION_TOO_LONG",
                exception.getLocale().languageTag(),
                exception.getMaxWords(),
                exception.getActualWords()));
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, VenueProfileInvalidException.class})
  public ResponseEntity<VenueProfileErrorResponse> handleInvalid() {
    return ResponseEntity.badRequest().body(new VenueProfileErrorResponse("VENUE_PROFILE_INVALID"));
  }

  @ExceptionHandler(VenueProfileNotFoundException.class)
  public ResponseEntity<VenueProfileErrorResponse> handleNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(new VenueProfileErrorResponse("VENUE_PROFILE_NOT_FOUND"));
  }

  @ExceptionHandler(VenueProfileForbiddenException.class)
  public ResponseEntity<VenueProfileErrorResponse> handleForbidden() {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new VenueProfileErrorResponse("VENUE_PROFILE_FORBIDDEN"));
  }

  @ExceptionHandler(VenueProfileConflictException.class)
  public ResponseEntity<VenueProfileErrorResponse> handleConflict() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new VenueProfileErrorResponse("VENUE_PROFILE_CONFLICT"));
  }
}
