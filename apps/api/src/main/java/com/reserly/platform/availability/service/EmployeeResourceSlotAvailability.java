package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.PublicEmployeeResourceAvailabilityResponse;
import java.util.List;

/** Resultado interno de aplicar los requisitos de servicio y recurso a una franja. */
public record EmployeeResourceSlotAvailability(
    boolean requirementsSatisfied,
    boolean employeeResourceRequired,
    boolean anyAvailableResourceAllowed,
    List<PublicEmployeeResourceAvailabilityResponse> availableEmployeeResources) {

  /** Una franja sin servicio no exige recursos de equipo. */
  public static EmployeeResourceSlotAvailability unrestricted() {
    return new EmployeeResourceSlotAvailability(true, false, false, List.of());
  }

  /** Un servicio inexistente o inactivo invalida la franja sin publicar detalles internos. */
  public static EmployeeResourceSlotAvailability unavailableService() {
    return new EmployeeResourceSlotAvailability(false, false, false, List.of());
  }
}
