package com.reserly.platform.reviews.controller;

import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato REST anónimo para valorar una reserva pasada propia. */
@RequestMapping(
    path = "/api/public/reservations",
    produces = MediaType.APPLICATION_JSON_VALUE)
public interface ReviewCreationController {

  /** Crea una reseña sin devolver identidad ni datos históricos de la reserva. */
  @PostMapping(
      path = "/{reservationId}/reviews",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<ReviewCreateResponse> create(
      @PathVariable UUID reservationId, @Valid @RequestBody ReviewCreateRequest request);
}
