package com.reserly.platform.reservations.service;

import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.incidents.service.VenueBookingRuleService;
import com.reserly.platform.reservations.dto.ManagedReservationResponse;
import com.reserly.platform.reservations.dto.ReservationCancellationResponse;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el secreto por SHA-256 y serializa la cancelación con lock pesimista.
 *
 * <p>Nunca registra, devuelve ni persiste el secreto recibido.
 */
@Service
public class ReservationManagementServiceImpl implements ReservationManagementService {

  private static final String CUSTOMER_CANCELLATION_REASON = "customer_request";

  private final ReservationDao reservationDao;
  private final OneTimeTokenService tokenService;
  private final ReservationCancellationPolicy cancellationPolicy;
  private final VenueBookingRuleService bookingRuleService;
  private final Clock clock;

  public ReservationManagementServiceImpl(
      ReservationDao reservationDao,
      OneTimeTokenService tokenService,
      ReservationCancellationPolicy cancellationPolicy,
      VenueBookingRuleService bookingRuleService,
      Clock clock) {
    this.reservationDao = reservationDao;
    this.tokenService = tokenService;
    this.cancellationPolicy = cancellationPolicy;
    this.bookingRuleService = bookingRuleService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public ManagedReservationResponse findByToken(String token) {
    ReservationEntity reservation =
        reservationDao
            .findBySecureTokenHash(requireHash(token))
            .filter(this::hasActiveManagementToken)
            .orElseThrow(ReservationManagementNotFoundException::new);
    return toResponse(reservation);
  }

  @Override
  @Transactional
  public ReservationCancellationResponse cancelByToken(String token) {
    ReservationEntity reservation =
        reservationDao
            .findBySecureTokenHashForUpdate(requireHash(token))
            .filter(this::hasActiveManagementToken)
            .orElseThrow(ReservationManagementNotFoundException::new);
    if (!"confirmed".equals(reservation.getStatus())) {
      throw new ReservationManagementNotFoundException();
    }
    var window = cancellationWindow(reservation);
    if (!window.allowed()) {
      throw new ReservationCancellationNotAllowedException();
    }
    Instant now = clock.instant();
    reservation.setStatus("cancelled_by_user");
    reservation.setCancelledAt(now);
    reservation.setCancelledBy("customer");
    reservation.setCancellationReason(CUSTOMER_CANCELLATION_REASON);
    reservation.setSecureTokenHash(null);
    reservation.setSecureTokenExpiresAt(null);
    reservation.setUpdatedAt(now);
    reservationDao.save(reservation);
    return new ReservationCancellationResponse(reservation.getStatus(), now);
  }

  private String requireHash(String token) {
    if (!tokenService.isValid(token)) {
      throw new ReservationManagementNotFoundException();
    }
    return tokenService.hash(token);
  }

  private boolean hasActiveManagementToken(ReservationEntity reservation) {
    return reservation.getSecureTokenExpiresAt() != null
        && reservation.getSecureTokenExpiresAt().isAfter(clock.instant());
  }

  private ManagedReservationResponse toResponse(ReservationEntity reservation) {
    var window = cancellationWindow(reservation);
    return new ManagedReservationResponse(
        reservation.getId(),
        reservation.getVenue().getName(),
        reservation.getVenue().getAddress(),
        reservation.getDate(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getPartySize(),
        reservation.getStatus(),
        "confirmed".equals(reservation.getStatus()) && window.allowed(),
        window.deadline(),
        resolvedCancellationRule(reservation).noticeMinutes());
  }

  private ReservationCancellationPolicy.CancellationWindow cancellationWindow(
      ReservationEntity reservation) {
    var rule = resolvedCancellationRule(reservation);
    return cancellationPolicy.evaluate(
        reservation.getDate(),
        reservation.getStartsAt(),
        rule.allowed(),
        rule.noticeMinutes(),
        clock.getZone(),
        clock.instant());
  }

  private VenueBookingRuleService.CancellationRule resolvedCancellationRule(
      ReservationEntity reservation) {
    return bookingRuleService.resolveCancellation(
        reservation.getVenue().getId(),
        reservation.getVenue().getCancellationNoticeMinutes());
  }
}
