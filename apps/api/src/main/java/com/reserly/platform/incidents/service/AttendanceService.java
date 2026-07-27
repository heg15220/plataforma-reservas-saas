package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.dto.AttendanceUpdateRequest;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.util.UUID;

/** Caso de uso transaccional de asistencia para el local autenticado. */
public interface AttendanceService {

  /**
   * Marca una reserva finalizada como asistida, no asistida o pendiente.
   *
   * <p>{@code pending} conserva el estado {@code confirmed} y registra la decisión manual para que
   * el job automático no la sobrescriba. No crea incidencias ni penalizaciones; esas acciones
   * requieren el flujo confirmado de reporte.
   *
   * @throws AttendanceNotFoundException si no existe o no pertenece al propietario
   * @throws AttendanceTooEarlyException si la reserva todavía no finalizó
   * @throws AttendanceInvalidException si el estado o la transición no son válidos
   */
  ReservationEntity update(
      UUID ownerUserId, UUID reservationId, AttendanceUpdateRequest request);
}
