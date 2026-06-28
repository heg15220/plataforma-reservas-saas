package com.reserly.platform.businessverification.service;

/**
 * Caso de uso interno que ejecuta y audita una comprobación empresarial remota.
 *
 * <p>No cambia el estado agregado de la cuenta. La tarea 1.8 consumirá el resultado técnico para
 * aplicar transiciones autorizadas.
 */
public interface RemoteBusinessVerificationService {

  /**
   * Comprueba una cuenta o recupera la evidencia existente para el mismo request.
   *
   * @throws BusinessAccountNotFoundException si la cuenta no existe
   */
  RemoteBusinessVerificationOutcome verify(RemoteBusinessVerificationCommand command);
}
