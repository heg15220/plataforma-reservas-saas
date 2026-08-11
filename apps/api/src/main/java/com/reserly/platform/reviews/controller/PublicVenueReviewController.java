package com.reserly.platform.reviews.controller;

import com.reserly.platform.reviews.dto.PublicVenueReviewCreateResponse;
import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewEligibilityRequest;
import com.reserly.platform.reviews.dto.ReviewEligibilityResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato público de elegibilidad y creación desde la ficha de un local publicado. */
@RequestMapping(
    path = "/api/public/venues/{venueSlug}/reviews",
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface PublicVenueReviewController {

  /** Comprueba elegibilidad sin devolver reservas, fechas, visitas ni historial del email. */
  @PostMapping(path = "/eligibility", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ReviewEligibilityResponse> checkEligibility(
      @PathVariable @Size(max = 160) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
          String venueSlug,
      @Valid @RequestBody ReviewEligibilityRequest request);

  /** Crea la reseña repitiendo la selección y validación en una transacción bloqueada. */
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<PublicVenueReviewCreateResponse> create(
      @PathVariable @Size(max = 160) @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$")
          String venueSlug,
      @Valid @RequestBody ReviewCreateRequest request);
}
