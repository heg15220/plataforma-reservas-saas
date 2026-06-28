package com.reserly.platform.businessverification.remote;

/** Frontera de ejecución remota con selección de proveedor, timeouts, reintentos e idempotencia. */
public interface RemoteBusinessVerificationGatewayService {

  /**
   * Ejecuta la comprobación con un proveedor compatible.
   *
   * @param request datos fiscales canónicos cargados por backend
   * @param preferredProvider código opcional; si se informa no se aplica fallback silencioso
   * @throws RemoteVerificationExecutionException fallo final normalizado
   */
  RemoteVerificationExecution verify(
      RemoteBusinessVerificationRequest request, String preferredProvider);
}
