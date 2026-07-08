package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.TimeSlotCapacityRequest;
import com.reserly.platform.availability.dto.TimeSlotGenerationRequest;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación de creación manual de franjas con validación contra horario y excepciones. */
@Service
public class TimeSlotServiceImpl implements TimeSlotService {

  private static final String STATUS_AVAILABLE = "available";
  private static final String STATUS_BLOCKED = "blocked";

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

    return timeSlotDao.saveAndFlush(newSlot(venue, weekday, request, false, Instant.now()));
  }

  @Override
  @Transactional
  public List<TimeSlotEntity> generate(UUID ownerUserId, TimeSlotGenerationRequest request) {
    validateGenerationRequest(request);
    VenueEntity venue = requireCurrentVenueForUpdate(ownerUserId);
    int weekday = request.date().getDayOfWeek().getValue();
    VenueOpeningHourEntity openingHour =
        openingHourDao
            .findOwnedByWeekday(ownerUserId, weekday)
            .orElseThrow(TimeSlotInvalidException::new);
    validateReservableDay(
        ownerUserId,
        new TimeSlotRequest(
            request.date(),
            openingHour.getOpensAt(),
            openingHour.getClosesAt(),
            request.capacity()),
        openingHour);

    List<TimeSlotRequest> candidates = buildGenerationCandidates(request, openingHour);
    if (candidates.isEmpty()) {
      throw new TimeSlotInvalidException();
    }
    for (TimeSlotRequest candidate : candidates) {
      if (timeSlotDao.existsOwnedOverlap(
          ownerUserId, candidate.date(), candidate.startsAt(), candidate.endsAt())) {
        throw new TimeSlotInvalidException();
      }
    }

    Instant now = Instant.now();
    List<TimeSlotEntity> slots =
        candidates.stream()
            .map(candidate -> newSlot(venue, weekday, candidate, true, now))
            .toList();
    return timeSlotDao.saveAllAndFlush(slots).stream()
        .sorted(Comparator.comparing(TimeSlotEntity::getStartsAt))
        .toList();
  }

  @Override
  @Transactional
  public TimeSlotEntity updateCapacity(
      UUID ownerUserId, UUID slotId, TimeSlotCapacityRequest request) {
    if (slotId == null || request == null || request.capacity() < 1) {
      throw new TimeSlotInvalidException();
    }
    TimeSlotEntity slot =
        timeSlotDao
            .findOwnedForUpdate(ownerUserId, slotId)
            .orElseThrow(TimeSlotInvalidException::new);
    slot.setCapacity(request.capacity());
    slot.setUpdatedAt(Instant.now());
    return timeSlotDao.saveAndFlush(slot);
  }

  @Override
  @Transactional
  public TimeSlotEntity block(UUID ownerUserId, UUID slotId) {
    TimeSlotEntity slot = requireOwnedSlotForUpdate(ownerUserId, slotId);
    slot.setStatus(STATUS_BLOCKED);
    slot.setUpdatedAt(Instant.now());
    return timeSlotDao.saveAndFlush(slot);
  }

  @Override
  @Transactional
  public TimeSlotEntity reopen(UUID ownerUserId, UUID slotId) {
    TimeSlotEntity slot = requireOwnedSlotForUpdate(ownerUserId, slotId);
    if (!STATUS_BLOCKED.equals(slot.getStatus())
        || availabilityBlockDao.existsOwnedDayOverride(ownerUserId, slot.getDate())) {
      throw new TimeSlotInvalidException();
    }
    slot.setStatus(STATUS_AVAILABLE);
    slot.setUpdatedAt(Instant.now());
    return timeSlotDao.saveAndFlush(slot);
  }

  private TimeSlotEntity requireOwnedSlotForUpdate(UUID ownerUserId, UUID slotId) {
    if (slotId == null) {
      throw new TimeSlotInvalidException();
    }
    return timeSlotDao
        .findOwnedForUpdate(ownerUserId, slotId)
        .orElseThrow(TimeSlotInvalidException::new);
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

  private void validateGenerationRequest(TimeSlotGenerationRequest request) {
    if (request == null) {
      throw new TimeSlotInvalidException();
    }
    validateDate(request.date());
    if (request.durationMinutes() < 5
        || request.durationMinutes() > 480
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

  private List<TimeSlotRequest> buildGenerationCandidates(
      TimeSlotGenerationRequest request, VenueOpeningHourEntity openingHour) {
    ArrayList<TimeSlotRequest> candidates = new ArrayList<>();
    LocalTime startsAt = openingHour.getOpensAt();
    while (true) {
      LocalTime endsAt = startsAt.plusMinutes(request.durationMinutes());
      if (endsAt.isAfter(openingHour.getClosesAt())) {
        break;
      }
      candidates.add(new TimeSlotRequest(request.date(), startsAt, endsAt, request.capacity()));
      startsAt = endsAt;
    }
    return candidates;
  }

  private TimeSlotEntity newSlot(
      VenueEntity venue, int weekday, TimeSlotRequest request, boolean createdByRule, Instant now) {
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setVenue(venue);
    slot.setDate(request.date());
    slot.setWeekday(weekday);
    slot.setStartsAt(request.startsAt());
    slot.setEndsAt(request.endsAt());
    slot.setCapacity(request.capacity());
    slot.setStatus(STATUS_AVAILABLE);
    slot.setCreatedByRule(createdByRule);
    slot.setVersion(0);
    slot.setCreatedAt(now);
    slot.setUpdatedAt(now);
    return slot;
  }
}
