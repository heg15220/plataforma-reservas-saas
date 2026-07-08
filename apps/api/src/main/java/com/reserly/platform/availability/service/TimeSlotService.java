package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.TimeSlotRequest;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Caso de uso privado para listar y crear franjas manuales del local vigente. */
public interface TimeSlotService {

  /** Lista las franjas de una fecha del local autenticado. */
  List<TimeSlotEntity> list(UUID ownerUserId, LocalDate date);

  /**
   * Crea una franja manual disponible.
   *
   * @throws TimeSlotInvalidException si la fecha no admite reservas o la franja no es válida
   */
  TimeSlotEntity create(UUID ownerUserId, TimeSlotRequest request);
}
