package com.reserly.platform.resources.service;

import com.reserly.platform.infrastructure.validation.PlainTextSanitizer;
import com.reserly.platform.resources.dto.EmployeeResourceCommand;
import com.reserly.platform.resources.dto.EmployeeResourceHourRequest;
import com.reserly.platform.resources.dto.EmployeeResourceWeeklyHoursRequest;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceHourDao;
import com.reserly.platform.resources.persistence.EmployeeResourceHourEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional del CRUD privado de equipo y recursos. */
@Service
public class EmployeeResourceCatalogServiceImpl implements EmployeeResourceCatalogService {

  private static final int MAX_FIRST_NAME_LENGTH = 120;
  private static final int MAX_LAST_NAME_LENGTH = 160;
  private static final int MAX_ALIAS_LENGTH = 160;
  private static final int MAX_PHOTO_URL_LENGTH = 2048;
  private static final int MAX_SPECIALTY_LENGTH = 240;
  private static final int MAX_LONG_TEXT_LENGTH = 2000;
  private static final Set<String> SUPPORTED_TYPES =
      Set.of("employee", "professional", "room", "court", "table", "equipment", "other");
  private static final Set<String> SUPPORTED_STATUSES =
      Set.of("active", "inactive", "internal_only", "archived");
  private static final Set<String> NON_PUBLIC_STATUSES = Set.of("internal_only", "archived");

  private final VenueDao venueDao;
  private final EmployeeResourceDao resourceDao;
  private final EmployeeResourceHourDao hourDao;

  public EmployeeResourceCatalogServiceImpl(
      VenueDao venueDao, EmployeeResourceDao resourceDao, EmployeeResourceHourDao hourDao) {
    this.venueDao = venueDao;
    this.resourceDao = resourceDao;
    this.hourDao = hourDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<EmployeeResourceEntity> list(UUID ownerUserId) {
    requireVenue(ownerUserId, false);
    return List.copyOf(resourceDao.findAllOwnedActiveCatalog(ownerUserId));
  }

  @Override
  @Transactional
  public EmployeeResourceEntity create(UUID ownerUserId, EmployeeResourceCommand command) {
    VenueEntity venue = requireVenue(ownerUserId, true);
    Instant now = Instant.now();
    EmployeeResourceEntity resource = new EmployeeResourceEntity();
    resource.setVenue(venue);
    resource.setCreatedAt(now);
    applyEditableFields(resource, command, now);
    return save(resource);
  }

  @Override
  @Transactional
  public EmployeeResourceEntity update(
      UUID ownerUserId, UUID resourceId, EmployeeResourceCommand command) {
    requireVenue(ownerUserId, true);
    EmployeeResourceEntity resource =
        resourceDao
            .findOwnedForUpdate(ownerUserId, resourceId)
            .orElseThrow(EmployeeResourceNotFoundException::new);
    applyEditableFields(resource, command, Instant.now());
    return save(resource);
  }

  @Override
  @Transactional(readOnly = true)
  public List<EmployeeResourceHourEntity> listWeeklyHours(UUID ownerUserId, UUID resourceId) {
    requireVenue(ownerUserId, false);
    requireOwnedResource(ownerUserId, resourceId);
    return List.copyOf(hourDao.findWeeklyHours(ownerUserId, resourceId));
  }

  @Override
  @Transactional
  public List<EmployeeResourceHourEntity> replaceWeeklyHours(
      UUID ownerUserId, UUID resourceId, EmployeeResourceWeeklyHoursRequest request) {
    requireVenue(ownerUserId, true);
    EmployeeResourceEntity resource = requireOwnedResourceForUpdate(ownerUserId, resourceId);
    List<EmployeeResourceHourEntity> existing =
        hourDao.findWeeklyHoursForUpdate(ownerUserId, resourceId);
    hourDao.deleteAll(existing);
    hourDao.flush();

    Instant now = Instant.now();
    List<EmployeeResourceHourEntity> replacement = toWeeklyHours(resource, request, now);
    return List.copyOf(hourDao.saveAllAndFlush(replacement));
  }

  private void applyEditableFields(
      EmployeeResourceEntity resource, EmployeeResourceCommand command, Instant updatedAt) {
    String type = normalizeCatalogValue(command.type(), SUPPORTED_TYPES);
    String status = normalizeCatalogValue(command.status(), SUPPORTED_STATUSES);
    String firstName = normalizeOptional(command.firstName(), MAX_FIRST_NAME_LENGTH);
    String publicAlias = normalizeOptional(command.publicAlias(), MAX_ALIAS_LENGTH);
    if (firstName == null && publicAlias == null) {
      throw new EmployeeResourceInvalidException();
    }
    resource.setType(type);
    resource.setFirstName(firstName);
    resource.setLastName(normalizeOptional(command.lastName(), MAX_LAST_NAME_LENGTH));
    resource.setPublicAlias(publicAlias);
    resource.setPhotoUrl(normalizeOptional(command.photoUrl(), MAX_PHOTO_URL_LENGTH));
    resource.setSpecialty(normalizeOptional(command.specialty(), MAX_SPECIALTY_LENGTH));
    resource.setDescription(normalizeOptional(command.description(), MAX_LONG_TEXT_LENGTH));
    resource.setStatus(status);
    resource.setPublicVisibility(resolvePublicVisibility(status, command.publicVisibility()));
    resource.setInternalNotes(normalizeOptional(command.internalNotes(), MAX_LONG_TEXT_LENGTH));
    resource.setUpdatedAt(updatedAt);
  }

  private String normalizeCatalogValue(String value, Set<String> allowedValues) {
    if (value == null) {
      throw new EmployeeResourceInvalidException();
    }
    String normalized = value.trim();
    if (!allowedValues.contains(normalized)) {
      throw new EmployeeResourceInvalidException();
    }
    return normalized;
  }

  private String normalizeOptional(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String normalized = PlainTextSanitizer.sanitize(value);
    if (normalized.isBlank() || normalized.length() > maxLength) {
      throw new EmployeeResourceInvalidException();
    }
    return normalized;
  }

  private boolean resolvePublicVisibility(String status, boolean requestedVisibility) {
    return requestedVisibility && !NON_PUBLIC_STATUSES.contains(status);
  }

  private VenueEntity requireVenue(UUID ownerUserId, boolean lock) {
    return (lock
            ? venueDao.findCurrentByOwnerUserIdForUpdate(ownerUserId)
            : venueDao.findCurrentByOwnerUserId(ownerUserId))
        .orElseThrow(EmployeeResourceNotFoundException::new);
  }

  private EmployeeResourceEntity requireOwnedResource(UUID ownerUserId, UUID resourceId) {
    return resourceDao
        .findOwned(ownerUserId, resourceId)
        .orElseThrow(EmployeeResourceNotFoundException::new);
  }

  private EmployeeResourceEntity requireOwnedResourceForUpdate(UUID ownerUserId, UUID resourceId) {
    return resourceDao
        .findOwnedForUpdate(ownerUserId, resourceId)
        .orElseThrow(EmployeeResourceNotFoundException::new);
  }

  private List<EmployeeResourceHourEntity> toWeeklyHours(
      EmployeeResourceEntity resource, EmployeeResourceWeeklyHoursRequest request, Instant now) {
    if (request == null || request.hours() == null) {
      throw new EmployeeResourceInvalidException();
    }
    Set<Integer> seenWeekdays = new HashSet<>();
    List<EmployeeResourceHourEntity> hours = new ArrayList<>();
    for (EmployeeResourceHourRequest day : request.hours()) {
      if (day == null || !seenWeekdays.add(day.weekday())) {
        throw new EmployeeResourceInvalidException();
      }
      hours.add(toWeeklyHour(resource, day, now));
    }
    return hours;
  }

  private EmployeeResourceHourEntity toWeeklyHour(
      EmployeeResourceEntity resource, EmployeeResourceHourRequest day, Instant now) {
    validateWeeklyHour(day);
    EmployeeResourceHourEntity hour = new EmployeeResourceHourEntity();
    hour.setEmployeeResource(resource);
    hour.setWeekday(day.weekday());
    hour.setAvailable(day.available());
    hour.setStartsAt(day.available() ? day.startsAt() : null);
    hour.setEndsAt(day.available() ? day.endsAt() : null);
    hour.setCreatedAt(now);
    hour.setUpdatedAt(now);
    return hour;
  }

  private void validateWeeklyHour(EmployeeResourceHourRequest day) {
    if (day.weekday() < 1 || day.weekday() > 7) {
      throw new EmployeeResourceInvalidException();
    }
    LocalTime startsAt = day.startsAt();
    LocalTime endsAt = day.endsAt();
    if (!day.available()) {
      if (startsAt != null || endsAt != null) {
        throw new EmployeeResourceInvalidException();
      }
      return;
    }
    if (startsAt == null || endsAt == null || !startsAt.isBefore(endsAt)) {
      throw new EmployeeResourceInvalidException();
    }
  }

  private EmployeeResourceEntity save(EmployeeResourceEntity resource) {
    try {
      return resourceDao.saveAndFlush(resource);
    } catch (DataIntegrityViolationException exception) {
      throw new EmployeeResourceInvalidException(exception);
    }
  }
}
