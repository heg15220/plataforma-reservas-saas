package com.reserly.platform.availability.service;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Resuelve requisitos de servicio y recursos para un conjunto de franjas publicadas. */
public interface EmployeeResourceAvailabilityService {

  /**
   * Cruza servicios, asociaciones y horario semanal sin realizar consultas individuales por franja.
   * La clave del resultado es el identificador estable de la franja recibida.
   */
  Map<UUID, EmployeeResourceSlotAvailability> resolve(
      UUID venueId, int weekday, List<TimeSlotEntity> slots);
}
