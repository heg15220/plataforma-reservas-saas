package com.reserly.platform.reviews.controller;

import com.reserly.platform.reviews.dto.PublicVenueReviewCreateResponse;
import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import com.reserly.platform.reviews.dto.ReviewEligibilityRequest;
import com.reserly.platform.reviews.dto.ReviewEligibilityResponse;
import com.reserly.platform.reviews.service.ReviewCreationService;
import com.reserly.platform.reviews.service.ReviewEligibilityService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador público fino; toda decisión de negocio se repite dentro de los servicios. */
@RestController
public class PublicVenueReviewControllerImpl implements PublicVenueReviewController {

  private final ReviewEligibilityService eligibilityService;
  private final ReviewCreationService creationService;

  public PublicVenueReviewControllerImpl(
      ReviewEligibilityService eligibilityService, ReviewCreationService creationService) {
    this.eligibilityService = eligibilityService;
    this.creationService = creationService;
  }

  @Override
  public ResponseEntity<ReviewEligibilityResponse> checkEligibility(
      String venueSlug, ReviewEligibilityRequest request) {
    return ResponseEntity.ok(eligibilityService.check(venueSlug, request));
  }

  @Override
  public ResponseEntity<PublicVenueReviewCreateResponse> create(
      String venueSlug, ReviewCreateRequest request) {
    ReviewCreateResponse created = creationService.createForVenue(venueSlug, request);
    return ResponseEntity.created(URI.create("/api/public/reviews/" + created.reviewId()))
        .body(PublicVenueReviewCreateResponse.from(created));
  }
}
