package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.dto.VenueBookingRuleUpdateRequest;
import com.reserly.platform.incidents.persistence.VenueBookingRuleEntity;
import java.util.UUID;

/** Casos de uso de configuración privada y resolución interna de reglas de reserva. */
public interface VenueBookingRuleService {

  /** Consulta las reglas del único local vigente del propietario autenticado. */
  VenueBookingRuleEntity get(UUID ownerUserId);

  /**
   * Reemplaza de forma serializada las reglas básicas de cancelación.
   *
   * @throws VenueBookingRuleInvalidException si la solicitud es nula o excede los límites
   */
  VenueBookingRuleEntity update(UUID ownerUserId, VenueBookingRuleUpdateRequest request);

  /**
   * Resuelve la regla que debe aplicar el flujo público de gestión.
   *
   * <p>El valor heredado solo actúa como compatibilidad defensiva si una base parcialmente migrada
   * todavía no contiene la fila sembrada por V27.
   */
  CancellationRule resolveCancellation(UUID venueId, int legacyNoticeMinutes);

  /** Snapshot inmutable consumido por el módulo de reservas sin exponer persistencia. */
  record CancellationRule(boolean allowed, int noticeMinutes) {}
}
