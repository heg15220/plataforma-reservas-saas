package com.reserly.platform.reviews.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.persistence.ReviewAggregateProjection;
import com.reserly.platform.reviews.persistence.ReviewDao;
import com.reserly.platform.reviews.persistence.ReviewEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;

/** Cobertura focalizada de elegibilidad, privacidad y unicidad de reseñas. */
class ReviewCreationServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-28T12:00:00Z");

  private final ReservationDao reservationDao = mock(ReservationDao.class);
  private final ReviewDao reviewDao = mock(ReviewDao.class);
  private final VenueDao venueDao = mock(VenueDao.class);
  private final Clock clock = Clock.fixed(NOW, ZoneId.of("Europe/Madrid"));
  private final ReviewCreationService service =
      new ReviewCreationServiceImpl(reservationDao, reviewDao, venueDao, clock);

  @BeforeEach
  void assignReviewIdWhenPersisted() {
    when(reviewDao.saveAndFlush(any(ReviewEntity.class)))
        .thenAnswer(
            invocation -> {
              ReviewEntity review = invocation.getArgument(0);
              review.setId(UUID.randomUUID());
              return review;
            });
    ReviewAggregateProjection aggregate = mock(ReviewAggregateProjection.class);
    when(aggregate.getAverageRating()).thenReturn(5.0);
    when(aggregate.getReviewsCount()).thenReturn(1L);
    when(reviewDao.summarizeByVenueId(any(UUID.class))).thenReturn(aggregate);
  }

  @Test
  void createsReviewForFinishedConfirmedReservationAndNormalizesInput() {
    ReservationEntity reservation = eligibleReservation("confirmed");
    when(reservationDao.findByIdForUpdate(reservation.getId()))
        .thenReturn(Optional.of(reservation));

    var response =
        service.create(
            reservation.getId(),
            new ReviewCreateRequest(" Customer@Example.COM ", 5, "  Atención excelente.  ", true));

    assertThat(response.status()).isEqualTo("created");
    assertThat(response.venueId()).isEqualTo(reservation.getVenue().getId());
    assertThat(response.reservationId()).isEqualTo(reservation.getId());
    assertThat(response.rating()).isEqualTo(5);
    assertThat(response.averageRating()).isEqualByComparingTo("5.0");
    assertThat(response.reviewsCount()).isEqualTo(1);

    ArgumentCaptor<ReviewEntity> review = ArgumentCaptor.forClass(ReviewEntity.class);
    verify(reviewDao).saveAndFlush(review.capture());
    assertThat(review.getValue().getCustomerEmailNormalized()).isEqualTo("customer@example.com");
    assertThat(review.getValue().getComment()).isEqualTo("Atención excelente.");
    assertThat(review.getValue().getCreatedAt()).isEqualTo(NOW);
    assertThat(review.getValue().getUpdatedAt()).isEqualTo(NOW);
  }

  @Test
  void acceptsFinishedAttendanceStatesAndConvertsBlankCommentToNull() {
    for (String status : new String[] {"attended", "no_show", "reported"}) {
      ReservationEntity reservation = eligibleReservation(status);
      when(reservationDao.findByIdForUpdate(reservation.getId()))
          .thenReturn(Optional.of(reservation));

      service.create(
          reservation.getId(), new ReviewCreateRequest("customer@example.com", 3, "   ", true));
    }

    ArgumentCaptor<ReviewEntity> reviews = ArgumentCaptor.forClass(ReviewEntity.class);
    verify(reviewDao, org.mockito.Mockito.times(3)).saveAndFlush(reviews.capture());
    assertThat(reviews.getAllValues()).allMatch(review -> review.getComment() == null);
  }

  @Test
  void rejectsMissingForeignOrCancelledReservationWithSameOpaqueError() {
    UUID missingId = UUID.randomUUID();
    when(reservationDao.findByIdForUpdate(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.create(missingId, validRequest()))
        .isInstanceOf(ReviewNotEligibleException.class);

    ReservationEntity foreign = eligibleReservation("confirmed");
    when(reservationDao.findByIdForUpdate(foreign.getId())).thenReturn(Optional.of(foreign));
    assertThatThrownBy(
            () ->
                service.create(
                    foreign.getId(), new ReviewCreateRequest("other@example.com", 5, null, true)))
        .isInstanceOf(ReviewNotEligibleException.class);

    ReservationEntity cancelled = eligibleReservation("cancelled_by_user");
    when(reservationDao.findByIdForUpdate(cancelled.getId())).thenReturn(Optional.of(cancelled));
    assertThatThrownBy(() -> service.create(cancelled.getId(), validRequest()))
        .isInstanceOf(ReviewNotEligibleException.class);

    verify(reviewDao, never()).saveAndFlush(any());
  }

  @Test
  void rejectsReservationThatHasNotFinishedInBusinessTimezone() {
    ReservationEntity reservation = eligibleReservation("confirmed");
    reservation.setDate(LocalDate.of(2026, 7, 28));
    reservation.setEndsAt(LocalTime.of(15, 0));
    when(reservationDao.findByIdForUpdate(reservation.getId()))
        .thenReturn(Optional.of(reservation));

    assertThatThrownBy(() -> service.create(reservation.getId(), validRequest()))
        .isInstanceOf(ReviewNotEligibleException.class);
    verify(reviewDao, never()).saveAndFlush(any());
  }

  @Test
  void rejectsSecondReviewBeforeWriting() {
    ReservationEntity reservation = eligibleReservation("confirmed");
    when(reservationDao.findByIdForUpdate(reservation.getId()))
        .thenReturn(Optional.of(reservation));
    when(reviewDao.existsByReservationId(reservation.getId())).thenReturn(true);

    assertThatThrownBy(() -> service.create(reservation.getId(), validRequest()))
        .isInstanceOf(ReviewAlreadySubmittedException.class);
    verify(reviewDao, never()).saveAndFlush(any());
  }

  @Test
  void translatesDatabaseUniqueRaceToStableConflict() {
    ReservationEntity reservation = eligibleReservation("confirmed");
    when(reservationDao.findByIdForUpdate(reservation.getId()))
        .thenReturn(Optional.of(reservation));
    when(reviewDao.saveAndFlush(any(ReviewEntity.class)))
        .thenThrow(new DataIntegrityViolationException("uqReviewsReservation"));

    assertThatThrownBy(() -> service.create(reservation.getId(), validRequest()))
        .isInstanceOf(ReviewAlreadySubmittedException.class)
        .hasCauseInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsInvalidDirectInvocationBeforeReadingReservation() {
    UUID reservationId = UUID.randomUUID();

    assertThatThrownBy(
            () ->
                service.create(
                    reservationId, new ReviewCreateRequest("customer@example.com", 0, null, true)))
        .isInstanceOf(ReviewInvalidException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    reservationId, new ReviewCreateRequest("customer@example.com", 5, null, false)))
        .isInstanceOf(ReviewInvalidException.class);
    assertThatThrownBy(
            () ->
                service.create(
                    reservationId, new ReviewCreateRequest("invalid-email", 5, null, true)))
        .isInstanceOf(ReviewInvalidException.class);

    verify(reservationDao, never()).findByIdForUpdate(any());
  }

  @Test
  void createsFromPublishedVenueUsingLatestLockedEligibleReservation() {
    ReservationEntity reservation = eligibleReservation("attended");
    VenueEntity venue = reservation.getVenue();
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(reservationDao.findLatestUnreviewedPastReviewEligibleReservationForUpdate(
            venue.getId(),
            "customer@example.com",
            ReviewEligibilityServiceImpl.ELIGIBLE_STATUSES,
            LocalDate.of(2026, 7, 28),
            LocalTime.of(14, 0),
            PageRequest.of(0, 1)))
        .thenReturn(List.of(reservation));

    var response = service.createForVenue("casa-luz", validRequest());

    assertThat(response.reservationId()).isEqualTo(reservation.getId());
    assertThat(response.venueId()).isEqualTo(venue.getId());
    verify(reviewDao).existsByReservationId(reservation.getId());
    verify(reviewDao).saveAndFlush(any(ReviewEntity.class));
  }

  @Test
  void distinguishesNoPastReservationFromAllEligibleReservationsReviewed() {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(reservationDao.findLatestUnreviewedPastReviewEligibleReservationForUpdate(
            venue.getId(),
            "customer@example.com",
            ReviewEligibilityServiceImpl.ELIGIBLE_STATUSES,
            LocalDate.of(2026, 7, 28),
            LocalTime.of(14, 0),
            PageRequest.of(0, 1)))
        .thenReturn(List.of());

    assertThatThrownBy(() -> service.createForVenue("casa-luz", validRequest()))
        .isInstanceOf(ReviewNotEligibleException.class);

    when(reservationDao.existsPastReviewEligibleReservation(
            venue.getId(),
            "customer@example.com",
            ReviewEligibilityServiceImpl.ELIGIBLE_STATUSES,
            LocalDate.of(2026, 7, 28),
            LocalTime.of(14, 0)))
        .thenReturn(true);
    assertThatThrownBy(() -> service.createForVenue("casa-luz", validRequest()))
        .isInstanceOf(ReviewAlreadySubmittedException.class);
    verify(reviewDao, never()).saveAndFlush(any());
  }

  @Test
  void revalidatesThatTheLockedCandidateBelongsToThePublishedVenue() {
    VenueEntity publishedVenue = new VenueEntity();
    publishedVenue.setId(UUID.randomUUID());
    ReservationEntity foreignReservation = eligibleReservation("confirmed");
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(publishedVenue));
    when(reservationDao.findLatestUnreviewedPastReviewEligibleReservationForUpdate(
            publishedVenue.getId(),
            "customer@example.com",
            ReviewEligibilityServiceImpl.ELIGIBLE_STATUSES,
            LocalDate.of(2026, 7, 28),
            LocalTime.of(14, 0),
            PageRequest.of(0, 1)))
        .thenReturn(List.of(foreignReservation));

    assertThatThrownBy(() -> service.createForVenue("casa-luz", validRequest()))
        .isInstanceOf(ReviewNotEligibleException.class);
    verify(reviewDao, never()).saveAndFlush(any());
  }

  private ReviewCreateRequest validRequest() {
    return new ReviewCreateRequest("customer@example.com", 4, "Buen servicio.", true);
  }

  private ReservationEntity eligibleReservation(String status) {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(UUID.randomUUID());
    reservation.setVenue(venue);
    reservation.setCustomerEmailNormalized("customer@example.com");
    reservation.setDate(LocalDate.of(2026, 7, 27));
    reservation.setEndsAt(LocalTime.of(18, 0));
    reservation.setStatus(status);
    return reservation;
  }
}
