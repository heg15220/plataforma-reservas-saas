package com.reserly.platform.reviews.service;

import com.reserly.platform.infrastructure.validation.PlainTextSanitizer;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reviews.dto.ReviewCreateRequest;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import com.reserly.platform.reviews.persistence.ReviewDao;
import com.reserly.platform.reviews.persistence.ReviewEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea reseñas bajo bloqueo de la reserva para serializar competidores por la misma visita.
 *
 * <p>El servicio repite todas las comprobaciones aunque exista una elegibilidad previa. Reserva
 * inexistente, email ajeno y estado no elegible producen el mismo error para evitar enumeración.
 */
@Service
public class ReviewCreationServiceImpl implements ReviewCreationService {

  private static final int MAX_COMMENT_LENGTH = 2000;
  private static final Set<String> ELIGIBLE_STATUSES =
      Set.of("confirmed", "attended", "no_show", "reported");

  private final ReservationDao reservationDao;
  private final ReviewDao reviewDao;
  private final VenueDao venueDao;
  private final Clock clock;

  public ReviewCreationServiceImpl(
      ReservationDao reservationDao, ReviewDao reviewDao, VenueDao venueDao, Clock clock) {
    this.reservationDao = reservationDao;
    this.reviewDao = reviewDao;
    this.venueDao = venueDao;
    this.clock = clock;
  }

  @Override
  @Transactional
  public ReviewCreateResponse create(UUID reservationId, ReviewCreateRequest request) {
    if (reservationId == null) {
      throw new ReviewInvalidException();
    }
    validateInput(request);
    String normalizedEmail = ReviewEligibilityServiceImpl.normalizeEmail(request.customerEmail());
    Instant now = clock.instant();
    ReservationEntity reservation =
        reservationDao
            .findByIdForUpdate(reservationId)
            .orElseThrow(ReviewNotEligibleException::new);

    requireEligibleReservation(reservation, normalizedEmail, now, null);
    if (reviewDao.existsByReservationId(reservationId)) {
      throw new ReviewAlreadySubmittedException();
    }
    return persistReview(reservation, request, normalizedEmail, now);
  }

  @Override
  @Transactional
  public ReviewCreateResponse createForVenue(String venueSlug, ReviewCreateRequest request) {
    validateInput(request);
    if (venueSlug == null || venueSlug.isBlank() || venueSlug.length() > 160) {
      throw new ReviewInvalidException();
    }
    String normalizedEmail = ReviewEligibilityServiceImpl.normalizeEmail(request.customerEmail());
    UUID venueId =
        venueDao
            .findPublishedBySlug(venueSlug)
            .map(venue -> venue.getId())
            .orElseThrow(ReviewNotEligibleException::new);
    LocalDate today = LocalDate.now(clock);
    LocalTime currentTime = LocalTime.now(clock);
    List<ReservationEntity> candidates =
        reservationDao.findLatestUnreviewedPastReviewEligibleReservationForUpdate(
            venueId, normalizedEmail, ELIGIBLE_STATUSES, today, currentTime, PageRequest.of(0, 1));
    if (candidates.isEmpty()) {
      if (reservationDao.existsPastReviewEligibleReservation(
          venueId, normalizedEmail, ELIGIBLE_STATUSES, today, currentTime)) {
        throw new ReviewAlreadySubmittedException();
      }
      throw new ReviewNotEligibleException();
    }

    ReservationEntity reservation = candidates.get(0);
    Instant now = clock.instant();
    requireEligibleReservation(reservation, normalizedEmail, now, venueId);
    if (reviewDao.existsByReservationId(reservation.getId())) {
      throw new ReviewAlreadySubmittedException();
    }
    return persistReview(reservation, request, normalizedEmail, now);
  }

  private ReviewCreateResponse persistReview(
      ReservationEntity reservation,
      ReviewCreateRequest request,
      String normalizedEmail,
      Instant now) {
    ReviewEntity review = new ReviewEntity();
    review.setVenueId(reservation.getVenue().getId());
    review.setReservationId(reservation.getId());
    review.setCustomerEmailNormalized(normalizedEmail);
    review.setRating(request.rating());
    review.setComment(normalizeComment(request.comment()));
    review.setCreatedAt(now);
    review.setUpdatedAt(now);

    try {
      review = reviewDao.saveAndFlush(review);
    } catch (DataIntegrityViolationException exception) {
      // La constraint única es la última defensa si otro escritor evita el bloqueo de aplicación.
      throw new ReviewAlreadySubmittedException(exception);
    }
    var aggregate = reviewDao.summarizeByVenueId(review.getVenueId());
    BigDecimal averageRating =
        BigDecimal.valueOf(aggregate.getAverageRating()).setScale(1, RoundingMode.HALF_UP);
    return new ReviewCreateResponse(
        "created",
        review.getId(),
        review.getVenueId(),
        review.getReservationId(),
        review.getRating(),
        averageRating,
        aggregate.getReviewsCount());
  }

  private void validateInput(ReviewCreateRequest request) {
    if (request == null
        || request.customerEmail() == null
        || request.customerEmail().isBlank()
        || request.customerEmail().length() > 320
        || !ReviewEligibilityServiceImpl.isValidEmail(request.customerEmail())
        || request.rating() < 1
        || request.rating() > 5
        || !request.acceptsReviewPolicy()
        || (request.comment() != null && request.comment().length() > MAX_COMMENT_LENGTH)) {
      throw new ReviewInvalidException();
    }
  }

  private void requireEligibleReservation(
      ReservationEntity reservation, String normalizedEmail, Instant now, UUID expectedVenueId) {
    if (reservation.getVenue() == null
        || reservation.getVenue().getId() == null
        || (expectedVenueId != null && !expectedVenueId.equals(reservation.getVenue().getId()))
        || reservation.getCustomerEmailNormalized() == null
        || !reservation.getCustomerEmailNormalized().equals(normalizedEmail)
        || !ELIGIBLE_STATUSES.contains(reservation.getStatus())
        || reservation.getDate() == null
        || reservation.getEndsAt() == null) {
      throw new ReviewNotEligibleException();
    }
    Instant end =
        LocalDateTime.of(reservation.getDate(), reservation.getEndsAt())
            .atZone(clock.getZone())
            .toInstant();
    if (end.isAfter(now)) {
      throw new ReviewNotEligibleException();
    }
  }

  private String normalizeComment(String comment) {
    return PlainTextSanitizer.sanitizeNullable(comment);
  }
}
