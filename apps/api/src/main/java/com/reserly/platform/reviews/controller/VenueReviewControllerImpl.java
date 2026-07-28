package com.reserly.platform.reviews.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reviews.dto.VenueReviewListResponse;
import com.reserly.platform.reviews.service.ReviewQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador privado que deriva el propietario únicamente del principal autenticado. */
@RestController
public class VenueReviewControllerImpl implements VenueReviewController {

  private final ReviewQueryService service;

  public VenueReviewControllerImpl(ReviewQueryService service) {
    this.service = service;
  }

  @Override
  public ResponseEntity<VenueReviewListResponse> list(
      AuthenticatedAccount account, int page, int size) {
    return ResponseEntity.ok(service.findOwned(account.userId(), page, size));
  }
}
