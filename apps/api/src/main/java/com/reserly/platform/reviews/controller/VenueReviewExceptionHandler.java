package com.reserly.platform.reviews.controller;

import com.reserly.platform.reviews.dto.ReviewErrorResponse;
import com.reserly.platform.reviews.service.VenueReviewInvalidPageException;
import com.reserly.platform.reviews.service.VenueReviewNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Errores privados estables sin revelar identificadores de local. */
@RestControllerAdvice(assignableTypes = VenueReviewControllerImpl.class)
public class VenueReviewExceptionHandler {

  @ExceptionHandler(VenueReviewInvalidPageException.class)
  ResponseEntity<ReviewErrorResponse> invalidPage() {
    return ResponseEntity.badRequest().body(new ReviewErrorResponse("VENUE_REVIEWS_INVALID_PAGE"));
  }

  @ExceptionHandler(VenueReviewNotFoundException.class)
  ResponseEntity<ReviewErrorResponse> notFound() {
    return ResponseEntity.notFound().build();
  }
}
