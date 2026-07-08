package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.OpeningHourRequest;
import com.reserly.platform.availability.dto.OpeningHoursUpdateRequest;
import com.reserly.platform.availability.persistence.VenueOpeningHourDao;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional del horario semanal del local vigente. */
@Service
public class OpeningHoursServiceImpl implements OpeningHoursService {

  private static final int WEEKDAY_MIN = 1;
  private static final int WEEKDAY_MAX = 7;
  private static final int REQUIRED_DAYS = 7;

  private final VenueDao venueDao;
  private final VenueOpeningHourDao openingHourDao;

  public OpeningHoursServiceImpl(VenueDao venueDao, VenueOpeningHourDao openingHourDao) {
    this.venueDao = venueDao;
    this.openingHourDao = openingHourDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueOpeningHourEntity> list(UUID ownerUserId) {
    requireCurrentVenue(ownerUserId);
    return openingHourDao.findAllOwned(ownerUserId);
  }

  @Override
  @Transactional
  public List<VenueOpeningHourEntity> replace(UUID ownerUserId, OpeningHoursUpdateRequest request) {
    VenueEntity venue = requireCurrentVenueForUpdate(ownerUserId);
    Map<Integer, VenueOpeningHourEntity> existingByWeekday = existingByWeekday(ownerUserId);
    Instant now = Instant.now();
    List<VenueOpeningHourEntity> updated = new ArrayList<>(REQUIRED_DAYS);

    for (OpeningHourRequest day : validateSnapshot(request)) {
      VenueOpeningHourEntity entity =
          existingByWeekday.getOrDefault(day.weekday(), new VenueOpeningHourEntity());
      if (entity.getId() == null) {
        entity.setVenue(venue);
        entity.setWeekday(day.weekday());
        entity.setCreatedAt(now);
      }
      entity.setClosed(day.closed());
      entity.setReservationsEnabled(!day.closed() && day.reservationsEnabled());
      entity.setOpensAt(day.closed() ? null : day.opensAt());
      entity.setClosesAt(day.closed() ? null : day.closesAt());
      entity.setUpdatedAt(now);
      updated.add(entity);
    }

    return openingHourDao.saveAllAndFlush(updated).stream()
        .sorted(Comparator.comparingInt(VenueOpeningHourEntity::getWeekday))
        .toList();
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

  private Map<Integer, VenueOpeningHourEntity> existingByWeekday(UUID ownerUserId) {
    Map<Integer, VenueOpeningHourEntity> existingByWeekday = new HashMap<>();
    for (VenueOpeningHourEntity existing : openingHourDao.findAllOwnedForUpdate(ownerUserId)) {
      existingByWeekday.put(existing.getWeekday(), existing);
    }
    return existingByWeekday;
  }

  private List<OpeningHourRequest> validateSnapshot(OpeningHoursUpdateRequest request) {
    if (request == null || request.days() == null || request.days().size() != REQUIRED_DAYS) {
      throw new OpeningHoursInvalidException();
    }

    HashSet<Integer> weekdays = new HashSet<>();
    List<OpeningHourRequest> sorted =
        request.days().stream()
            .sorted(Comparator.comparingInt(OpeningHourRequest::weekday))
            .toList();
    for (OpeningHourRequest day : sorted) {
      validateDay(day);
      if (!weekdays.add(day.weekday())) {
        throw new OpeningHoursInvalidException();
      }
    }
    if (!weekdays.containsAll(List.of(1, 2, 3, 4, 5, 6, 7))) {
      throw new OpeningHoursInvalidException();
    }
    return sorted;
  }

  private void validateDay(OpeningHourRequest day) {
    if (day.weekday() < WEEKDAY_MIN || day.weekday() > WEEKDAY_MAX) {
      throw new OpeningHoursInvalidException();
    }
    if (day.closed()) {
      if (day.reservationsEnabled() || day.opensAt() != null || day.closesAt() != null) {
        throw new OpeningHoursInvalidException();
      }
      return;
    }
    if (day.opensAt() == null
        || day.closesAt() == null
        || !day.opensAt().isBefore(day.closesAt())) {
      throw new OpeningHoursInvalidException();
    }
  }
}
