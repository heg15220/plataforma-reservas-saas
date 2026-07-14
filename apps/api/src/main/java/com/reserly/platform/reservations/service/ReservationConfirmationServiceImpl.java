package com.reserly.platform.reservations.service;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ReservationConfirmRequest;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.persistence.ReservationTimeSlotDao;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirma el agregado bajo locks de reserva y franja, sin persistir aún respuestas personalizadas. */
@Service
public class ReservationConfirmationServiceImpl implements ReservationConfirmationService {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  private final ReservationDao reservationDao;
  private final ReservationTimeSlotDao timeSlotDao;
  private final OneTimeTokenService tokenService;
  private final Clock clock;

  @Autowired
  public ReservationConfirmationServiceImpl(
      ReservationDao reservationDao,
      ReservationTimeSlotDao timeSlotDao,
      OneTimeTokenService tokenService) {
    this(reservationDao, timeSlotDao, tokenService, Clock.systemUTC());
  }

  ReservationConfirmationServiceImpl(
      ReservationDao reservationDao,
      ReservationTimeSlotDao timeSlotDao,
      OneTimeTokenService tokenService,
      Clock clock) {
    this.reservationDao = reservationDao;
    this.timeSlotDao = timeSlotDao;
    this.tokenService = tokenService;
    this.clock = clock;
  }

  /**
   * Adquiere primero el lock del agregado y después el de su franja. Las tareas 7.7 a 7.10
   * ampliarán las políticas específicas de expiración, capacidad, formulario y token de gestión.
   */
  @Override
  @Transactional
  public ReservationConfirmResponse confirm(
      UUID reservationId, ReservationConfirmRequest request) {
    validateRequest(reservationId, request);
    ReservationEntity reservation =
        reservationDao
            .findByIdForUpdate(reservationId)
            .orElseThrow(ReservationConfirmationInvalidException::new);
    Instant now = clock.instant();
    if (!"hold".equals(reservation.getStatus())
        || !tokenMatches(request.holdToken(), reservation.getHoldTokenHash())
        || reservation.getHoldExpiresAt() == null
        || !now.isBefore(reservation.getHoldExpiresAt())
        || request.partySize() != reservation.getPartySize()) {
      throw new ReservationConfirmationInvalidException();
    }

    TimeSlotEntity slot =
        timeSlotDao
            .findByIdForUpdate(reservation.getTimeSlot().getId())
            .orElseThrow(ReservationConfirmationInvalidException::new);
    long occupiedCapacity = reservationDao.sumOccupiedCapacity(slot.getId(), now);
    if (occupiedCapacity > slot.getCapacity()) {
      throw new ReservationConfirmationInvalidException();
    }

    String customerName = request.customerName().strip();
    String customerEmail = request.customerEmail().strip();
    reservation.setCustomerName(customerName);
    reservation.setCustomerEmail(customerEmail);
    reservation.setCustomerEmailNormalized(customerEmail.toLowerCase(Locale.ROOT));
    reservation.setStatus("confirmed");
    reservation.setHoldExpiresAt(null);
    reservation.setHoldTokenHash(null);
    reservation.setUpdatedAt(now);
    ReservationEntity saved = reservationDao.save(reservation);

    return new ReservationConfirmResponse(
        saved.getStatus(),
        saved.getId(),
        saved.getCustomerEmail(),
        saved.getVenue().getName(),
        saved.getDate(),
        saved.getStartsAt(),
        saved.getEndsAt(),
        saved.getPartySize());
  }

  private void validateRequest(UUID reservationId, ReservationConfirmRequest request) {
    if (reservationId == null
        || request == null
        || !request.acceptsPrivacyPolicy()
        || !request.acceptsBookingRules()
        || request.partySize() < 1
        || request.customerName() == null
        || request.customerName().isBlank()
        || request.customerName().strip().length() > 160
        || request.customerEmail() == null
        || request.customerEmail().isBlank()
        || request.customerEmail().strip().length() > 320
        || !EMAIL_PATTERN.matcher(request.customerEmail().strip()).matches()
        || request.formResponses() == null
        || !request.formResponses().isEmpty()) {
      throw new ReservationConfirmationInvalidException();
    }
  }

  private boolean tokenMatches(String rawToken, String storedHash) {
    if (!tokenService.isValid(rawToken) || storedHash == null) {
      return false;
    }
    String candidateHash = tokenService.hash(rawToken);
    return MessageDigest.isEqual(
        storedHash.getBytes(StandardCharsets.US_ASCII),
        candidateHash.getBytes(StandardCharsets.US_ASCII));
  }
}
