package com.reserly.platform.reviews.controller;

import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import com.reserly.platform.reviews.service.ReviewCreationService;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP fino; la elegibilidad y concurrencia permanecen en el servicio. */
@RestController
public class ReviewCreationControllerImpl implements ReviewCreationController {

  private final ReviewCreationService service;

  public ReviewCreationControllerImpl(ReviewCreationService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<ReviewCreateResponse> create(
      UUID reservationId, ReviewCreateRequest request) {
    ReviewCreateResponse response = service.create(reservationId, request);
    return ResponseEntity.created(URI.create("/api/public/reviews/" + response.reviewId()))
        .body(response);
  }
}
