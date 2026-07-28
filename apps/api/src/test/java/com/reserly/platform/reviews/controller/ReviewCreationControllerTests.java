package com.reserly.platform.reviews.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import com.reserly.platform.reviews.service.ReviewCreationService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Verifica el contrato HTTP mínimo sin cargar el contexto completo de Spring. */
class ReviewCreationControllerTests {

  @Test
  void returnsCreatedReviewWithoutIdentityData() {
    ReviewCreationService service = org.mockito.Mockito.mock(ReviewCreationService.class);
    UUID reservationId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();
    ReviewCreateRequest request =
        new ReviewCreateRequest("customer@example.com", 5, "Excelente.", true);
    ReviewCreateResponse response =
        new ReviewCreateResponse(
            "created", reviewId, venueId, reservationId, 5, new BigDecimal("4.7"), 12);
    when(service.create(reservationId, request)).thenReturn(response);

    var result = new ReviewCreationControllerImpl(service).create(reservationId, request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.getHeaders().getLocation()).hasToString("/api/public/reviews/" + reviewId);
    assertThat(result.getBody()).isEqualTo(response);
    verify(service).create(reservationId, request);
  }
}
