package com.reserly.platform.reviews.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import com.reserly.platform.reviews.dto.ReviewEligibilityRequest;
import com.reserly.platform.reviews.dto.ReviewEligibilityResponse;
import com.reserly.platform.reviews.service.ReviewCreationService;
import com.reserly.platform.reviews.service.ReviewEligibilityService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Verifica los contratos minimizados de la ficha sin arrancar el contexto completo. */
class PublicVenueReviewControllerTests {

  private final ReviewEligibilityService eligibilityService = mock(ReviewEligibilityService.class);
  private final ReviewCreationService creationService = mock(ReviewCreationService.class);
  private final PublicVenueReviewController controller =
      new PublicVenueReviewControllerImpl(eligibilityService, creationService);

  @Test
  void returnsEligibilityDecisionWithoutReservationData() {
    ReviewEligibilityRequest request = new ReviewEligibilityRequest("guest@example.com");
    when(eligibilityService.check("casa-luz", request))
        .thenReturn(ReviewEligibilityResponse.allowed());

    var result = controller.checkEligibility("casa-luz", request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(ReviewEligibilityResponse.allowed());
    assertThat(result.getBody().toString())
        .doesNotContain("guest@example.com", "reservationId", "date", "visits");
    verify(eligibilityService).check("casa-luz", request);
  }

  @Test
  void createsFromVenueWithoutExposingTheSelectedReservation() {
    ReviewCreateRequest request =
        new ReviewCreateRequest("guest@example.com", 5, "Excelente.", true);
    UUID reviewId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();
    UUID privateReservationId = UUID.randomUUID();
    when(creationService.createForVenue("casa-luz", request))
        .thenReturn(
            new ReviewCreateResponse(
                "created",
                reviewId,
                venueId,
                privateReservationId,
                5,
                new BigDecimal("4.8"),
                12));

    var result = controller.create("casa-luz", request);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.getHeaders().getLocation()).hasToString("/api/public/reviews/" + reviewId);
    assertThat(result.getBody().reviewId()).isEqualTo(reviewId);
    assertThat(result.getBody().toString()).doesNotContain(privateReservationId.toString());
    verify(creationService).createForVenue("casa-luz", request);
  }
}
