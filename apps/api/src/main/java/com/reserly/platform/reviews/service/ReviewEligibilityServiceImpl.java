package com.reserly.platform.reviews.service;

import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reviews.dto.ReviewEligibilityRequest;
import com.reserly.platform.reviews.dto.ReviewEligibilityResponse;
import com.reserly.platform.venues.persistence.VenueDao;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comprueba elegibilidad con respuestas opacas y consultas booleanas acotadas.
 *
 * <p>Un slug inexistente/no publicado comparte el rechazo sin reserva. El servicio nunca devuelve
 * identificadores, fechas, recuentos ni estados de las reservas encontradas.
 */
@Service
public class ReviewEligibilityServiceImpl implements ReviewEligibilityService {

  static final Set<String> ELIGIBLE_STATUSES =
      Set.of("confirmed", "attended", "no_show", "reported");
  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final VenueDao venueDao;
  private final ReservationDao reservationDao;
  private final Clock clock;

  public ReviewEligibilityServiceImpl(
      VenueDao venueDao, ReservationDao reservationDao, Clock clock) {
    this.venueDao = venueDao;
    this.reservationDao = reservationDao;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public ReviewEligibilityResponse check(String venueSlug, ReviewEligibilityRequest request) {
    validate(venueSlug, request);
    String normalizedEmail = normalizeEmail(request.customerEmail());
    UUID venueId =
        venueDao.findPublishedBySlug(venueSlug).map(venue -> venue.getId()).orElse(null);
    if (venueId == null) {
      return ReviewEligibilityResponse.notEligible();
    }

    LocalDate today = LocalDate.now(clock);
    LocalTime currentTime = LocalTime.now(clock);
    if (reservationDao.existsUnreviewedPastReviewEligibleReservation(
        venueId, normalizedEmail, ELIGIBLE_STATUSES, today, currentTime)) {
      return ReviewEligibilityResponse.allowed();
    }
    if (reservationDao.existsPastReviewEligibleReservation(
        venueId, normalizedEmail, ELIGIBLE_STATUSES, today, currentTime)) {
      return ReviewEligibilityResponse.alreadySubmitted();
    }
    return ReviewEligibilityResponse.notEligible();
  }

  private void validate(String venueSlug, ReviewEligibilityRequest request) {
    if (venueSlug == null
        || venueSlug.isBlank()
        || venueSlug.length() > 160
        || request == null
        || request.customerEmail() == null
        || request.customerEmail().isBlank()
        || request.customerEmail().length() > 320
        || !isValidEmail(request.customerEmail())) {
      throw new ReviewInvalidException();
    }
  }

  static String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  static boolean isValidEmail(String email) {
    return EMAIL_PATTERN.matcher(email.strip()).matches();
  }
}
