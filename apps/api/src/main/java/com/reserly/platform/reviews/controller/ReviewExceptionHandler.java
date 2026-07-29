package com.reserly.platform.reviews.controller;

import com.reserly.platform.reviews.dto.ReviewErrorResponse;
import com.reserly.platform.reviews.service.ReviewAlreadySubmittedException;
import com.reserly.platform.reviews.service.ReviewInvalidException;
import com.reserly.platform.reviews.service.ReviewNotEligibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce rechazos de reseña sin revelar reservas, fechas ni emails registrados. */
@RestControllerAdvice(
    basePackageClasses = {ReviewCreationController.class, PublicVenueReviewController.class})
public class ReviewExceptionHandler {

  @ExceptionHandler({MethodArgumentNotValidException.class, ReviewInvalidException.class})
  ResponseEntity<ReviewErrorResponse> invalid() {
    return ResponseEntity.badRequest().body(new ReviewErrorResponse("VALIDATION_ERROR"));
  }

  @ExceptionHandler(ReviewNotEligibleException.class)
  ResponseEntity<ReviewErrorResponse> notEligible() {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
        .body(new ReviewErrorResponse("REVIEW_NOT_ELIGIBLE"));
  }

  @ExceptionHandler(ReviewAlreadySubmittedException.class)
  ResponseEntity<ReviewErrorResponse> alreadySubmitted() {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ReviewErrorResponse("REVIEW_ALREADY_SUBMITTED"));
  }
}
