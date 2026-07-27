package com.reserly.platform.incidents.service;

import com.reserly.platform.incidents.dto.NoShowReportRequest;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import java.util.UUID;

/** Caso de uso de reporte confirmado y auditado de una no asistencia. */
public interface NoShowReportService {

  /**
   * Registra la incidencia y su auditoría en la misma transacción que cambia la reserva.
   *
   * @throws NoShowReportNotFoundException si la reserva no pertenece al propietario
   * @throws NoShowReportInvalidException si falta la confirmación explícita
   * @throws NoShowReportStateException si la reserva no está en {@code no_show}
   */
  NoShowIncidentEntity report(
      UUID ownerUserId,
      UUID reservationId,
      NoShowReportRequest request,
      NoShowReportAuditContext auditContext);
}
