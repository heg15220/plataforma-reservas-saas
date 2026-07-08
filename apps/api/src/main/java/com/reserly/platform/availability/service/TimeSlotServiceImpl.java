package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.persistence.AvailabilityBlockDao;
import com.reserly.platform.availability.persistence.TimeSlotDao;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.persistence.VenueOpeningHourDao;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación de creación manual de franjas con validación contra horario y excepciones. */
@Service
public class TimeSlotServiceImpl implements TimeSlotService {

  private static final String STATUS_AVAILABLE = "available";

  private final VenueDao venueDao;
  private final VenueOpeningHourDao openingHourDao;
  private final AvailabilityBlockDao availabilityBlockDao;
  private final TimeSlotDao timeSlotDao;

  public TimeSlotServiceImpl(
      VenueDao venueDao,
      VenueOpeningHourDao openingHourDao,
      AvailabilityBlockDao availabilityBlockDao,
      TimeSlotDao timeSlotDao) {
    this.venueDao = venueDao;
    this.openingHourDao = openingHourDao;
    this.availabilityBlockDao = availabilityBlockDao;
    this.timeSlotDao = timeSlotDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TimeSlotEntity> list(UUID ownerUserId, LocalDate date) {
    validateDate(date);
    requireCurrentVenue(ownerUserId);
    return timeSlotDao.findAllOwnedByDate(ownerUserId, date);
  }

  @Override
  @Transactional
  public TimeSlotEntity create(UUID ownerUserId, TimeSlotRequest request) {
    validateRequest(request);
    VenueEntity venue = requireCurrentVenueForUpdate(ownerUserId);
    int weekday = request.date().getDayOfWeek().getValue();
    VenueOpeningHourEntity openingHour =
        openingHourDao
            .findOwnedByWeekday(ownerUserId, weekday)
            .orElseThrow(TimeSlotInvalidException::new);
    validateReservableDay(ownerUserId, request, openingHour);
    if (timeSlotDao.existsOwnedOverlap(
        ownerUserId, request.date(), request.startsAt(), request.endsAt())) {
      throw new TimeSlotInvalidException();
    }

    Instant now = Instant.now();
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setVenue(venue);
    slot.setDate(request.date());
    slot.setWeekday(weekday);
    slot.setStartsAt(request.startsAt());
    slot.setEndsAt(request.endsAt());
    slot.setCapacity(request.capacity());
    slot.setStatus(STATUS_AVAILABLE);
    slot.setCreatedByRule(false);
    slot.setVersion(0);
    slot.setCreatedAt(now);
    slot.setUpdatedAt(now);
    return timeSlotDao.saveAndFlush(slot);
  }

  private VenueEntity requireCurrentVenue(UUID ownerUserId) {
    return venueDao
        .findCurrentByOwnerUserId(ownerUserId)
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  private VenueEntity requireCurrentVenueForUpdate(UUID ownerUserId) {
    return venueDao
        .findCurrentByOwnerUserIdForUpdate(ownerUserId)
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  private void validateRequest(TimeSlotRequest request) {
    if (request == null) {
      throw new TimeSlotInvalidException();
    }
    validateDate(request.date());
    if (request.startsAt() == null
        || request.endsAt() == null
        || !request.startsAt().isBefore(request.endsAt())
        || request.capacity() < 1) {
      throw new TimeSlotInvalidException();
    }
  }

  private void validateDate(LocalDate date) {
    if (date == null) {
      throw new TimeSlotInvalidException();
    }
  }

  private void validateReservableDay(
      UUID ownerUserId, TimeSlotRequest request, VenueOpeningHourEntity openingHour) {
    if (openingHour.isClosed()
        || !openingHour.isReservationsEnabled()
        || availabilityBlockDao.existsOwnedDayOverride(ownerUserId, request.date())
        || request.startsAt().isBefore(openingHour.getOpensAt())
        || request.endsAt().isAfter(openingHour.getClosesAt())) {
      throw new TimeSlotInvalidException();
    }
  }
}
