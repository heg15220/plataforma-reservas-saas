package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.PublicEmployeeResourceAvailabilityResponse;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceEntity;
import com.reserly.platform.resources.persistence.EmployeeResourceHourDao;
import com.reserly.platform.resources.persistence.EmployeeResourceHourEntity;
import com.reserly.platform.services.persistence.ServiceDao;
import com.reserly.platform.services.persistence.ServiceEntity;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Implementa la interseccion de servicio compatible y horario semanal del recurso. */
@Service
public class EmployeeResourceAvailabilityServiceImpl
    implements EmployeeResourceAvailabilityService {

  private final ServiceDao serviceDao;
  private final EmployeeResourceHourDao hourDao;

  public EmployeeResourceAvailabilityServiceImpl(
      ServiceDao serviceDao, EmployeeResourceHourDao hourDao) {
    this.serviceDao = serviceDao;
    this.hourDao = hourDao;
  }

  @Override
  public Map<UUID, EmployeeResourceSlotAvailability> resolve(
      UUID venueId, int weekday, List<TimeSlotEntity> slots) {
    Set<UUID> serviceIds =
        slots.stream()
            .map(TimeSlotEntity::getServiceId)
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toUnmodifiableSet());
    Map<UUID, ServiceEntity> services = loadServices(venueId, serviceIds);
    Map<UUID, EmployeeResourceHourEntity> hours = loadHours(venueId, weekday, serviceIds);
    Map<UUID, EmployeeResourceSlotAvailability> resolved = new HashMap<>();
    for (TimeSlotEntity slot : slots) {
      resolved.put(slot.getId(), resolveSlot(slot, services, hours));
    }
    return Map.copyOf(resolved);
  }

  private Map<UUID, ServiceEntity> loadServices(UUID venueId, Set<UUID> serviceIds) {
    if (serviceIds.isEmpty()) {
      return Map.of();
    }
    return serviceDao.findPublishedActiveWithResources(venueId, serviceIds).stream()
        .collect(Collectors.toUnmodifiableMap(ServiceEntity::getId, Function.identity()));
  }

  private Map<UUID, EmployeeResourceHourEntity> loadHours(
      UUID venueId, int weekday, Set<UUID> serviceIds) {
    if (serviceIds.isEmpty()) {
      return Map.of();
    }
    return hourDao.findPublishedAvailableHours(venueId, weekday).stream()
        .collect(
            Collectors.toUnmodifiableMap(
                hour -> hour.getEmployeeResource().getId(), Function.identity()));
  }

  private EmployeeResourceSlotAvailability resolveSlot(
      TimeSlotEntity slot,
      Map<UUID, ServiceEntity> services,
      Map<UUID, EmployeeResourceHourEntity> hours) {
    if (slot.getServiceId() == null) {
      return EmployeeResourceSlotAvailability.unrestricted();
    }
    ServiceEntity service = services.get(slot.getServiceId());
    if (service == null) {
      return EmployeeResourceSlotAvailability.unavailableService();
    }
    Set<EmployeeResourceEntity> compatibleResources = service.getCompatibleResources();
    if (compatibleResources.isEmpty()) {
      return EmployeeResourceSlotAvailability.unrestricted();
    }
    List<PublicEmployeeResourceAvailabilityResponse> availableResources =
        compatibleResources.stream()
            .filter(resource -> covers(hours.get(resource.getId()), slot))
            .map(this::toPublicResource)
            .sorted(
                Comparator.comparing(
                        PublicEmployeeResourceAvailabilityResponse::displayName,
                        String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(PublicEmployeeResourceAvailabilityResponse::employeeResourceId))
            .toList();
    boolean anyAvailableAllowed =
        service.isAnyAvailableResourceAllowed() && !availableResources.isEmpty();
    return new EmployeeResourceSlotAvailability(
        !availableResources.isEmpty(), true, anyAvailableAllowed, availableResources);
  }

  private boolean covers(EmployeeResourceHourEntity hour, TimeSlotEntity slot) {
    return hour != null
        && !hour.getStartsAt().isAfter(slot.getStartsAt())
        && !hour.getEndsAt().isBefore(slot.getEndsAt());
  }

  private PublicEmployeeResourceAvailabilityResponse toPublicResource(
      EmployeeResourceEntity resource) {
    return new PublicEmployeeResourceAvailabilityResponse(
        resource.getId(), resource.getType(), publicDisplayName(resource), resource.getSpecialty());
  }

  private String publicDisplayName(EmployeeResourceEntity resource) {
    if (resource.getPublicAlias() != null && !resource.getPublicAlias().isBlank()) {
      return resource.getPublicAlias();
    }
    return resource.getFirstName();
  }
}
