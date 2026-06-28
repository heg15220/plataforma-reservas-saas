package com.reserly.platform.businessverification.service;

/**
 * Caso de uso interno que ejecuta y audita una comprobación empresarial remota.
 *
 * <p>Inicia la transición remota, ejecuta la red sin transacción abierta y aplica la evidencia
 * auditada mediante la máquina de estados.
 */
public interface RemoteBusinessVerificationService {

  /**
   * Comprueba una cuenta o recupera la evidencia existente para el mismo request.
   *
   * @throws BusinessAccountNotFoundException si la cuenta no existe
   */
  RemoteBusinessVerificationOutcome verify(RemoteBusinessVerificationCommand command);
}
