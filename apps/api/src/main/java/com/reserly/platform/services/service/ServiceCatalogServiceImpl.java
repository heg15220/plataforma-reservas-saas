package com.reserly.platform.services.service;

import com.reserly.platform.resources.persistence.EmployeeResourceDao;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.services.dto.ServiceCommand;
import com.reserly.platform.services.dto.ServiceResourceAssignmentRequest;
import com.reserly.platform.services.persistence.ServiceDao;
import com.reserly.platform.services.persistence.ServiceEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementacion transaccional del catalogo privado de servicios. */
@Service
public class ServiceCatalogServiceImpl implements ServiceCatalogService {

  private static final int MAX_NAME_LENGTH = 160;
  private static final int MAX_DESCRIPTION_LENGTH = 2000;
  private static final int MIN_DURATION_MINUTES = 1;
  private static final int MAX_DURATION_MINUTES = 1440;

  private final VenueDao venueDao;
  private final ServiceDao serviceDao;
  private final EmployeeResourceDao resourceDao;

  public ServiceCatalogServiceImpl(
      VenueDao venueDao, ServiceDao serviceDao, EmployeeResourceDao resourceDao) {
    this.venueDao = venueDao;
    this.serviceDao = serviceDao;
    this.resourceDao = resourceDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ServiceEntity> list(UUID ownerUserId) {
    requireVenue(ownerUserId, false);
    return List.copyOf(serviceDao.findAllOwned(ownerUserId));
  }

  @Override
  @Transactional
  public ServiceEntity create(UUID ownerUserId, ServiceCommand command) {
    VenueEntity venue = requireVenue(ownerUserId, true);
    Instant now = Instant.now();
    ServiceEntity service = new ServiceEntity();
    service.setVenue(venue);
    service.setCreatedAt(now);
    applyEditableFields(service, command, now);
    return save(service);
  }

  @Override
  @Transactional
  public ServiceEntity update(UUID ownerUserId, UUID serviceId, ServiceCommand command) {
    requireVenue(ownerUserId, true);
    ServiceEntity service =
        serviceDao
            .findOwnedForUpdate(ownerUserId, serviceId)
            .orElseThrow(ServiceNotFoundException::new);
    applyEditableFields(service, command, Instant.now());
    return save(service);
  }

  @Override
  @Transactional
  public ServiceEntity replaceCompatibleResources(
      UUID ownerUserId, UUID serviceId, ServiceResourceAssignmentRequest request) {
    requireVenue(ownerUserId, true);
    ServiceEntity service =
        serviceDao
            .findOwnedWithResourcesForUpdate(ownerUserId, serviceId)
            .orElseThrow(ServiceNotFoundException::new);
    Set<UUID> requestedIds = normalizeRequestedResourceIds(request);
    List<EmployeeResourceEntity> resources =
        requestedIds.isEmpty()
            ? List.of()
            : resourceDao.findAllOwnedAssignable(ownerUserId, requestedIds);
    if (resources.size() != requestedIds.size()) {
      throw new ServiceInvalidException();
    }
    service.setCompatibleResources(new HashSet<>(resources));
    service.setUpdatedAt(Instant.now());
    return save(service);
  }

  private void applyEditableFields(
      ServiceEntity service, ServiceCommand command, Instant updatedAt) {
    String name = normalizeRequired(command.name(), MAX_NAME_LENGTH);
    String description = normalizeOptional(command.description(), MAX_DESCRIPTION_LENGTH);
    validateDuration(command.durationMinutes());
    if (command.capacityRequired() < 1) {
      throw new ServiceInvalidException();
    }
    service.setName(name);
    service.setNameI18n(command.nameI18n());
    service.setDescription(description);
    service.setDescriptionI18n(command.descriptionI18n());
    service.setDurationMinutes(command.durationMinutes());
    service.setCapacityRequired(command.capacityRequired());
    service.setActive(command.active());
    service.setUpdatedAt(updatedAt);
  }

  private String normalizeRequired(String value, int maxLength) {
    if (value == null) {
      throw new ServiceInvalidException();
    }
    String normalized = value.trim();
    if (normalized.isBlank() || normalized.length() > maxLength) {
      throw new ServiceInvalidException();
    }
    return normalized;
  }

  private String normalizeOptional(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.isBlank() || normalized.length() > maxLength) {
      throw new ServiceInvalidException();
    }
    return normalized;
  }

  private void validateDuration(int durationMinutes) {
    if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
      throw new ServiceInvalidException();
    }
  }

  private VenueEntity requireVenue(UUID ownerUserId, boolean lock) {
    return (lock
            ? venueDao.findCurrentByOwnerUserIdForUpdate(ownerUserId)
            : venueDao.findCurrentByOwnerUserId(ownerUserId))
        .orElseThrow(ServiceNotFoundException::new);
  }

  private Set<UUID> normalizeRequestedResourceIds(ServiceResourceAssignmentRequest request) {
    if (request == null || request.resourceIds() == null) {
      throw new ServiceInvalidException();
    }
    if (request.resourceIds().stream().anyMatch(id -> id == null)) {
      throw new ServiceInvalidException();
    }
    return Set.copyOf(request.resourceIds());
  }

  private ServiceEntity save(ServiceEntity service) {
    try {
      return serviceDao.saveAndFlush(service);
    } catch (DataIntegrityViolationException exception) {
      throw new ServiceInvalidException(exception);
    }
  }
}
