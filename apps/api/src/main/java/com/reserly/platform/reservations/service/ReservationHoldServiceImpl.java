package com.reserly.platform.reservations.service;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.service.EmployeeResourceAssignmentException;
import com.reserly.platform.availability.service.EmployeeResourceAssignmentService;
import com.reserly.platform.availability.service.ResourceAssignmentPreference;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.persistence.ReservationTimeSlotDao;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación inicial de creación de holds sobre una franja pública vigente. */
@Service
public class ReservationHoldServiceImpl implements ReservationHoldService {

  private static final Duration INITIAL_HOLD_DURATION = Duration.ofMinutes(5);

  private final ReservationTimeSlotDao timeSlotDao;
  private final ReservationDao reservationDao;
  private final EmployeeResourceAssignmentService assignmentService;
  private final OneTimeTokenService tokenService;
  private final Clock clock;

  @Autowired
  public ReservationHoldServiceImpl(
      ReservationTimeSlotDao timeSlotDao,
      ReservationDao reservationDao,
      EmployeeResourceAssignmentService assignmentService,
      OneTimeTokenService tokenService) {
    this(timeSlotDao, reservationDao, assignmentService, tokenService, Clock.systemUTC());
  }

  ReservationHoldServiceImpl(
      ReservationTimeSlotDao timeSlotDao,
      ReservationDao reservationDao,
      EmployeeResourceAssignmentService assignmentService,
      OneTimeTokenService tokenService,
      Clock clock) {
    this.timeSlotDao = timeSlotDao;
    this.reservationDao = reservationDao;
    this.assignmentService = assignmentService;
    this.tokenService = tokenService;
    this.clock = clock;
  }

  /**
   * Mantiene una única transacción y persiste un secreto hasheado. El bloqueo pesimista y el
   * descuento de otros holds corresponden expresamente a 7.4 y 7.5.
   */
  @Override
  @Transactional
  public ReservationHoldResponse create(ReservationHoldRequest request) {
    if (request == null) {
      throw new ReservationHoldInvalidException();
    }
    TimeSlotEntity slot =
        timeSlotDao
            .findPublished(request.venueId(), request.timeSlotId())
            .orElseThrow(ReservationHoldInvalidException::new);
    validateSlot(request, slot);

    Optional<UUID> assignedResource;
    try {
      assignedResource =
          assignmentService.assign(
              request.venueId(),
              slot.getWeekday(),
              slot,
              parsePreference(request.assignmentPreference()),
              request.employeeResourceId());
    } catch (EmployeeResourceAssignmentException exception) {
      throw new ReservationHoldInvalidException();
    }

    Instant now = clock.instant();
    Instant expiresAt = now.plus(INITIAL_HOLD_DURATION);
    String rawToken = tokenService.generate();
    ReservationEntity reservation = new ReservationEntity();
    reservation.setVenue(slot.getVenue());
    reservation.setTimeSlot(slot);
    reservation.setServiceId(slot.getServiceId());
    reservation.setEmployeeResourceId(assignedResource.orElse(null));
    reservation.setPartySize(request.partySize());
    reservation.setDate(slot.getDate());
    reservation.setStartsAt(slot.getStartsAt());
    reservation.setEndsAt(slot.getEndsAt());
    reservation.setStatus("hold");
    reservation.setHoldExpiresAt(expiresAt);
    reservation.setHoldTokenHash(tokenService.hash(rawToken));
    reservation.setCreatedAt(now);
    reservation.setUpdatedAt(now);
    ReservationEntity saved = reservationDao.save(reservation);

    return new ReservationHoldResponse(
        saved.getId(), rawToken, expiresAt, INITIAL_HOLD_DURATION.toSeconds());
  }

  private void validateSlot(ReservationHoldRequest request, TimeSlotEntity slot) {
    Instant now = clock.instant();
    LocalDate today = now.atZone(clock.getZone()).toLocalDate();
    LocalTime currentTime = now.atZone(clock.getZone()).toLocalTime();
    boolean past =
        slot.getDate().isBefore(today)
            || (slot.getDate().isEqual(today) && !slot.getStartsAt().isAfter(currentTime));
    if (!"available".equals(slot.getStatus())
        || past
        || request.partySize() < 1
        || request.partySize() > slot.getCapacity()
        || !Objects.equals(request.serviceId(), slot.getServiceId())) {
      throw new ReservationHoldInvalidException();
    }
  }

  private ResourceAssignmentPreference parsePreference(String value) {
    if (value == null) {
      return null;
    }
    try {
      return ResourceAssignmentPreference.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new ReservationHoldInvalidException();
    }
  }
}
