package com.reserly.platform.venues.controller;

import com.reserly.platform.forms.controller.PublicReservationFormControllerImpl;
import com.reserly.platform.venues.dto.VenueDescriptionLimitErrorResponse;
import com.reserly.platform.venues.dto.VenueProfileErrorResponse;
import com.reserly.platform.venues.dto.VenuePublicationErrorResponse;
import com.reserly.platform.venues.image.VenueImageStorageException;
import com.reserly.platform.venues.image.VenueImageValidationException;
import com.reserly.platform.venues.service.VenueCustomTabInvalidException;
import com.reserly.platform.venues.service.VenueCustomTabLimitException;
import com.reserly.platform.venues.service.VenueDescriptionTooLongException;
import com.reserly.platform.venues.service.VenueGalleryLimitException;
import com.reserly.platform.venues.service.VenueProfileConflictException;
import com.reserly.platform.venues.service.VenueProfileForbiddenException;
import com.reserly.platform.venues.service.VenueProfileInvalidException;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import com.reserly.platform.venues.service.VenuePublicationRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

/** Traduce errores del perfil a códigos estables sin publicar IDs ni constraints. */
@RestControllerAdvice(
    assignableTypes = {
      VenueProfileControllerImpl.class,
      VenueMainImageControllerImpl.class,
      VenueGalleryControllerImpl.class,
      VenueCustomTabControllerImpl.class,
      VenueEmailAssignmentControllerImpl.class,
      VenuePublicProfileControllerImpl.class,
      PublicReservationFormControllerImpl.class
    })
public class VenueProfileExceptionHandler {

  @ExceptionHandler(VenuePublicationRejectedException.class)
  public ResponseEntity<VenuePublicationErrorResponse> handlePublicationRejected(
      VenuePublicationRejectedException exception) {
    return ResponseEntity.status(422)
        .body(
            new VenuePublicationErrorResponse(
                "VENUE_PUBLICATION_REJECTED",
                exception.getRequirements().stream().map(Enum::name).sorted().toList()));
  }

  @ExceptionHandler(VenueGalleryLimitException.class)
  public ResponseEntity<VenueProfileErrorResponse> handleGalleryLimit() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new VenueProfileErrorResponse("VENUE_GALLERY_LIMIT_REACHED"));
  }

  @ExceptionHandler(VenueCustomTabLimitException.class)
  public ResponseEntity<VenueProfileErrorResponse> handleCustomTabLimit() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new VenueProfileErrorResponse("VENUE_CUSTOM_TAB_LIMIT_REACHED"));
  }

  @ExceptionHandler(VenueCustomTabInvalidException.class)
  public ResponseEntity<VenueProfileErrorResponse> handleInvalidCustomTab() {
    return ResponseEntity.badRequest()
        .body(new VenueProfileErrorResponse("VENUE_CUSTOM_TAB_INVALID"));
  }

  @ExceptionHandler({VenueImageValidationException.class, MultipartException.class})
  public ResponseEntity<VenueProfileErrorResponse> handleInvalidImage() {
    return ResponseEntity.badRequest().body(new VenueProfileErrorResponse("VENUE_IMAGE_INVALID"));
  }

  @ExceptionHandler(VenueImageStorageException.class)
  public ResponseEntity<VenueProfileErrorResponse> handleImageStorageUnavailable() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new VenueProfileErrorResponse("VENUE_IMAGE_STORAGE_UNAVAILABLE"));
  }

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
