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

  private final ReservationTimeSlotDao timeSlotDao;
  private final ReservationDao reservationDao;
  private final EmployeeResourceAssignmentService assignmentService;
  private final OneTimeTokenService tokenService;
  private final Clock clock;
  private final ReservationHoldExpirationPolicy expirationPolicy;

  @Autowired
  public ReservationHoldServiceImpl(
      ReservationTimeSlotDao timeSlotDao,
      ReservationDao reservationDao,
      EmployeeResourceAssignmentService assignmentService,
      OneTimeTokenService tokenService,
      ReservationHoldExpirationPolicy expirationPolicy) {
    this(
        timeSlotDao,
        reservationDao,
        assignmentService,
        tokenService,
        expirationPolicy,
        Clock.systemUTC());
  }

  ReservationHoldServiceImpl(
      ReservationTimeSlotDao timeSlotDao,
      ReservationDao reservationDao,
      EmployeeResourceAssignmentService assignmentService,
      OneTimeTokenService tokenService,
      ReservationHoldExpirationPolicy expirationPolicy,
      Clock clock) {
    this.timeSlotDao = timeSlotDao;
    this.reservationDao = reservationDao;
    this.assignmentService = assignmentService;
    this.tokenService = tokenService;
    this.expirationPolicy = expirationPolicy;
    this.clock = clock;
  }

  /**
   * Bloquea la franja, descuenta ocupación efectiva y persiste el secreto exclusivamente como hash.
   * La suma se ejecuta después de adquirir el lock y antes de asignar recursos.
   */
  @Override
  @Transactional
  public ReservationHoldResponse create(ReservationHoldRequest request) {
    if (request == null) {
      throw new ReservationHoldInvalidException();
    }
    TimeSlotEntity slot =
        timeSlotDao
            .findPublishedForUpdate(request.venueId(), request.timeSlotId())
            .orElseThrow(ReservationHoldInvalidException::new);
    Instant now = clock.instant();
    validateSlot(request, slot, now);
    long occupiedCapacity = reservationDao.sumOccupiedCapacity(slot.getId(), now);
    if (occupiedCapacity + request.partySize() > slot.getCapacity()) {
      throw new ReservationHoldInvalidException();
    }

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

    Instant expiresAt = expirationPolicy.expiresAt(now);
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
        saved.getId(), rawToken, expiresAt, expirationPolicy.remainingSeconds(expiresAt, now));
  }

  private void validateSlot(ReservationHoldRequest request, TimeSlotEntity slot, Instant now) {
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
