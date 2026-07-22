package com.reserly.platform.reservations.service;

import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ManagedReservationResponse;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resuelve el secreto por SHA-256 sin registrarlo, devolverlo ni persistirlo en claro. */
@Service
public class ReservationManagementServiceImpl implements ReservationManagementService {

  private final ReservationDao reservationDao;
  private final OneTimeTokenService tokenService;
  private final Clock clock;

  public ReservationManagementServiceImpl(
      ReservationDao reservationDao, OneTimeTokenService tokenService, Clock clock) {
    this.reservationDao = reservationDao;
    this.tokenService = tokenService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public ManagedReservationResponse findByToken(String token) {
    if (!tokenService.isValid(token)) {
      throw new ReservationManagementNotFoundException();
    }
    ReservationEntity reservation =
        reservationDao
            .findBySecureTokenHash(tokenService.hash(token))
            .filter(
                value ->
                    value.getSecureTokenExpiresAt() != null
                        && value.getSecureTokenExpiresAt().isAfter(clock.instant()))
            .orElseThrow(ReservationManagementNotFoundException::new);
    return new ManagedReservationResponse(
        reservation.getId(),
        reservation.getVenue().getName(),
        reservation.getVenue().getAddress(),
        reservation.getDate(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getPartySize(),
        reservation.getStatus());
  }
}
