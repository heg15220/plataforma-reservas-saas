package com.reserly.platform.availability.service;

import com.reserly.platform.availability.dto.AvailabilityDayRequest;
import com.reserly.platform.availability.dto.AvailabilityDayResponse;
import java.time.LocalDate;
import java.util.UUID;

/** Caso de uso privado para consultar y reemplazar excepciones de una fecha concreta. */
public interface AvailabilityDayService {

  /** Devuelve el estado configurado o derivado del horario semanal para la fecha. */
  AvailabilityDayResponse find(UUID ownerUserId, LocalDate date);

  /**
   * Sustituye la excepción de la fecha.
   *
   * @throws AvailabilityDayInvalidException si la fecha o flags son incoherentes
   */
  AvailabilityDayResponse replace(UUID ownerUserId, AvailabilityDayRequest request);
}
