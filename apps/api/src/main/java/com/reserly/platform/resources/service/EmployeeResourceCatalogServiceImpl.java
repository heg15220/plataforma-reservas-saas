package com.reserly.platform.resources.service;

import com.reserly.platform.resources.dto.EmployeeResourceCommand;
import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
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

  public EmployeeResourceCatalogServiceImpl(VenueDao venueDao, EmployeeResourceDao resourceDao) {
    this.venueDao = venueDao;
    this.resourceDao = resourceDao;
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
    String normalized = value.trim();
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

  private EmployeeResourceEntity save(EmployeeResourceEntity resource) {
    try {
      return resourceDao.saveAndFlush(resource);
    } catch (DataIntegrityViolationException exception) {
      throw new EmployeeResourceInvalidException(exception);
    }
  }
}
