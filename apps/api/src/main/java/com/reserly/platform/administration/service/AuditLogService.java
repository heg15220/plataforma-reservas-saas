package com.reserly.platform.administration.service;

import com.reserly.platform.administration.persistence.AuditLogEntity;

/** Puerto interno para añadir evidencia a la transacción de una acción crítica. */
public interface AuditLogService {

  /**
   * Persiste una entrada validada dentro de la transacción del llamador.
   *
   * @throws IllegalArgumentException si faltan actor, entidad, acción o snapshots válidos
   */
  AuditLogEntity record(AuditLogEntry entry);
}
