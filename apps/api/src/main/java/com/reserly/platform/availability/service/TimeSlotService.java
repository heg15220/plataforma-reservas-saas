package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.TimeSlotCapacityRequest;
import com.reserly.platform.availability.dto.TimeSlotGenerationRequest;
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
   * Elimina todas las franjas propias de una fecha.
   *
   * @throws TimeSlotDeleteConflictException si alguna franja conserva reservas asociadas
   */
  void deleteByDate(UUID ownerUserId, LocalDate date);

  /**
   * Crea una franja manual disponible.
   *
   * @throws TimeSlotInvalidException si la fecha no admite reservas o la franja no es válida
   */
  TimeSlotEntity create(UUID ownerUserId, TimeSlotRequest request);

  /**
   * Genera franjas automáticas para una fecha usando duración fija.
   *
   * @throws TimeSlotInvalidException si la fecha no admite reservas o alguna franja se solapa
   */
  List<TimeSlotEntity> generate(UUID ownerUserId, TimeSlotGenerationRequest request);

  /** Actualiza la capacidad máxima de una franja propia. */
  TimeSlotEntity updateCapacity(UUID ownerUserId, UUID slotId, TimeSlotCapacityRequest request);

  /** Bloquea manualmente una franja propia para impedir nuevas reservas. */
  TimeSlotEntity block(UUID ownerUserId, UUID slotId);

  /** Reabre una franja bloqueada manualmente si su día no está cerrado. */
  TimeSlotEntity reopen(UUID ownerUserId, UUID slotId);
}
