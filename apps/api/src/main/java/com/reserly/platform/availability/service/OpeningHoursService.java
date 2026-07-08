package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.OpeningHoursUpdateRequest;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso privado para consultar y sustituir el horario semanal del local autenticado.
 *
 * <p>El contrato exige siete días ISO-8601. La propiedad del local no procede nunca del payload.
 */
public interface OpeningHoursService {

  /** Devuelve el horario del local vigente del propietario en orden lunes-domingo. */
  List<VenueOpeningHourEntity> list(UUID ownerUserId);

  /**
   * Sustituye de forma atómica el snapshot semanal.
   *
   * @throws OpeningHoursInvalidException si faltan días, hay duplicados o las horas no son válidas
   */
  List<VenueOpeningHourEntity> replace(UUID ownerUserId, OpeningHoursUpdateRequest request);
}
