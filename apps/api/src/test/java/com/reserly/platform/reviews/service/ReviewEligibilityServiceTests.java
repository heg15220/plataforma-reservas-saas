package com.reserly.platform.reviews.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reviews.dto.ReviewEligibilityRequest;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Cobertura de elegibilidad por local/email y minimización de la decisión pública. */
class ReviewEligibilityServiceTests {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-07-28T12:00:00Z"), ZoneId.of("Europe/Madrid"));

  private final VenueDao venueDao = mock(VenueDao.class);
  private final ReservationDao reservationDao = mock(ReservationDao.class);
  private final ReviewEligibilityService service =
      new ReviewEligibilityServiceImpl(venueDao, reservationDao, CLOCK);

  @Test
  void acceptsNormalizedEmailWhenAnUnreviewedPastReservationExists() {
    VenueEntity venue = venue();
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(reservationDao.existsUnreviewedPastReviewEligibleReservation(
            venue.getId(),
            "guest@example.com",
            ReviewEligibilityServiceImpl.ELIGIBLE_STATUSES,
            LocalDate.of(2026, 7, 28),
            LocalTime.of(14, 0)))
        .thenReturn(true);

    var response =
        service.check("casa-luz", new ReviewEligibilityRequest(" Guest@Example.COM "));

    assertThat(response.eligible()).isTrue();
    assertThat(response.canReview()).isTrue();
    assertThat(response.error()).isNull();
    assertThat(response.messageKey()).isNull();
    assertThat(response.toString())
        .doesNotContain("guest@example.com", "reservation", "date", "visit");
  }

  @Test
  void returnsOpaqueNotEligibleForMissingVenueOrPastReservation() {
    var missingVenue =
        service.check("oculto", new ReviewEligibilityRequest("guest@example.com"));

    assertThat(missingVenue.error()).isEqualTo("REVIEW_NOT_ELIGIBLE");
    verify(reservationDao, never())
        .existsPastReviewEligibleReservation(
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());

    VenueEntity venue = venue();
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    var noReservation =
        service.check("casa-luz", new ReviewEligibilityRequest("guest@example.com"));

    assertThat(noReservation.eligible()).isFalse();
    assertThat(noReservation.error()).isEqualTo("REVIEW_NOT_ELIGIBLE");
    assertThat(noReservation.messageKey()).isEqualTo("reviews.notEligibleForVenue");
  }

  @Test
  void distinguishesWhenEveryPastEligibleReservationAlreadyHasAReview() {
    VenueEntity venue = venue();
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(reservationDao.existsPastReviewEligibleReservation(
            venue.getId(),
            "guest@example.com",
            ReviewEligibilityServiceImpl.ELIGIBLE_STATUSES,
            LocalDate.of(2026, 7, 28),
            LocalTime.of(14, 0)))
        .thenReturn(true);

    var response =
        service.check("casa-luz", new ReviewEligibilityRequest("guest@example.com"));

    assertThat(response.eligible()).isFalse();
    assertThat(response.canReview()).isFalse();
    assertThat(response.error()).isEqualTo("REVIEW_ALREADY_SUBMITTED");
    assertThat(response.messageKey()).isEqualTo("reviews.alreadySubmittedForVenue");
  }

  @Test
  void rejectsInvalidDirectInputBeforeQueries() {
    assertThatThrownBy(() -> service.check("", new ReviewEligibilityRequest("guest@example.com")))
        .isInstanceOf(ReviewInvalidException.class);
    assertThatThrownBy(() -> service.check("casa-luz", null))
        .isInstanceOf(ReviewInvalidException.class);
    assertThatThrownBy(
            () -> service.check("casa-luz", new ReviewEligibilityRequest("invalid-email")))
        .isInstanceOf(ReviewInvalidException.class);
    verify(venueDao, never()).findPublishedBySlug(org.mockito.ArgumentMatchers.any());
  }

  private VenueEntity venue() {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    return venue;
  }
}
