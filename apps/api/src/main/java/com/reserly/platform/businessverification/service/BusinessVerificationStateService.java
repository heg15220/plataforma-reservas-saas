package com.reserly.platform.businessverification.service;

import java.time.Instant;
import java.util.UUID;

/** Puerto transaccional de la máquina de estados de verificación empresarial. */
public interface BusinessVerificationStateService {

  /**
   * Reserva una cuenta para una comprobación remota.
   *
   * @throws BusinessVerificationInProgressException si otra operación sigue activa
   */
  BusinessVerificationStateSnapshot beginRemoteCheck(UUID businessAccountId, UUID requestId);

  /**
   * Aplica una evidencia auditada a la operación activa.
   *
   * @throws BusinessVerificationStateConflictException si la evidencia no pertenece a la operación
   */
  BusinessVerificationStateSnapshot completeRemoteCheck(
      UUID businessAccountId, UUID requestId, UUID verificationCheckId);

  /** Recupera el resumen vigente sin exponer datos fiscales. */
  BusinessVerificationStateSnapshot current(UUID businessAccountId);

  /** Caduca aprobaciones cuya vigencia haya terminado y devuelve el número afectado. */
  int expireDueVerifications(Instant now);
}
