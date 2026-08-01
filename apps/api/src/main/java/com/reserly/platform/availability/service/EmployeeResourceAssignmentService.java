package com.reserly.platform.availability.service;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import java.util.Optional;
import java.util.UUID;

/** Selecciona un recurso valido para una franja antes de crear el futuro hold de reserva. */
public interface EmployeeResourceAssignmentService {

  /**
   * Recalcula candidatos y resuelve una seleccion concreta o la primera disponibilidad.
   *
   * @return identificador asignado, o vacio cuando la franja no requiere recurso
   * @throws EmployeeResourceAssignmentException si la preferencia no esta permitida o no existe
   *     candidato elegible
   */
  Optional<UUID> assign(
      UUID venueId,
      int weekday,
      TimeSlotEntity slot,
      ResourceAssignmentPreference preference,
      UUID selectedResourceId);
}
