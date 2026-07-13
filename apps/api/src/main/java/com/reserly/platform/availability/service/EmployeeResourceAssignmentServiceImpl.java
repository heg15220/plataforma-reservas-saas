package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.PublicEmployeeResourceAvailabilityResponse;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Implementa asignacion determinista sobre la disponibilidad efectiva mas reciente. */
@Service
public class EmployeeResourceAssignmentServiceImpl implements EmployeeResourceAssignmentService {

  private final EmployeeResourceAvailabilityService availabilityService;

  public EmployeeResourceAssignmentServiceImpl(
      EmployeeResourceAvailabilityService availabilityService) {
    this.availabilityService = availabilityService;
  }

  @Override
  public Optional<UUID> assign(
      UUID venueId,
      int weekday,
      TimeSlotEntity slot,
      ResourceAssignmentPreference preference,
      UUID selectedResourceId) {
    if (venueId == null || slot == null || weekday < 1 || weekday > 7) {
      throw new EmployeeResourceAssignmentException();
    }
    EmployeeResourceSlotAvailability availability =
        availabilityService
            .resolve(venueId, weekday, List.of(slot))
            .getOrDefault(slot.getId(), EmployeeResourceSlotAvailability.unavailableService());
    if (!availability.employeeResourceRequired()) {
      if (preference != null || selectedResourceId != null) {
        throw new EmployeeResourceAssignmentException();
      }
      return Optional.empty();
    }
    if (!availability.requirementsSatisfied()) {
      throw new EmployeeResourceAssignmentException();
    }
    if (preference == ResourceAssignmentPreference.ANY_AVAILABLE) {
      return assignFirstAvailable(availability, selectedResourceId);
    }
    if (preference == ResourceAssignmentPreference.SPECIFIC) {
      return assignSpecific(availability, selectedResourceId);
    }
    throw new EmployeeResourceAssignmentException();
  }

  private Optional<UUID> assignFirstAvailable(
      EmployeeResourceSlotAvailability availability, UUID selectedResourceId) {
    if (selectedResourceId != null || !availability.anyAvailableResourceAllowed()) {
      throw new EmployeeResourceAssignmentException();
    }
    return availability.availableEmployeeResources().stream()
        .findFirst()
        .map(PublicEmployeeResourceAvailabilityResponse::employeeResourceId);
  }

  private Optional<UUID> assignSpecific(
      EmployeeResourceSlotAvailability availability, UUID selectedResourceId) {
    if (selectedResourceId == null) {
      throw new EmployeeResourceAssignmentException();
    }
    boolean available =
        availability.availableEmployeeResources().stream()
            .map(PublicEmployeeResourceAvailabilityResponse::employeeResourceId)
            .anyMatch(selectedResourceId::equals);
    if (!available) {
      throw new EmployeeResourceAssignmentException();
    }
    return Optional.of(selectedResourceId);
  }
}