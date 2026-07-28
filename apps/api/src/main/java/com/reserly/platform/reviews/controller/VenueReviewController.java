package com.reserly.platform.reviews.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reviews.dto.VenueReviewListResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Contrato privado de reseñas recibidas por el local autenticado. */
@RequestMapping(path = "/api/venue/me/reviews", produces = MediaType.APPLICATION_JSON_VALUE)
public interface VenueReviewController {

  /** Lista reseñas propias con páginas basadas en cero y tamaño limitado a cien. */
  @GetMapping
  ResponseEntity<VenueReviewListResponse> list(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size);
}
