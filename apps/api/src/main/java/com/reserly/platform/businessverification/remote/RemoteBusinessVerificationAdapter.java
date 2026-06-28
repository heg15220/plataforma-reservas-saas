package com.reserly.platform.businessverification.remote;

import java.util.Set;

/**
 * Puerto de salida para un registro fiscal o mercantil remoto.
 *
 * <p>Una implementación concreta debe aplicar autenticación, timeouts del contexto, minimización de
 * respuesta y propagación de idempotencia cuando el proveedor lo permita.
 */
public interface RemoteBusinessVerificationAdapter {

  /** Código técnico estable, en minúsculas, usado en configuración y auditoría. */
  String providerCode();

  /** Países ISO alpha-2 que el adaptador puede comprobar. */
  Set<String> supportedCountries();

  /**
   * Prioridad ascendente para selección automática. Los proveedores oficiales y gratuitos deben
   * usar valores menores que alternativas comerciales equivalentes.
   */
  int priority();

  /**
   * Ejecuta una comprobación remota sin modificar entidades ni estados locales.
   *
   * @throws RemoteBusinessVerificationException fallo remoto normalizado
   */
  RemoteBusinessVerificationResult verify(
      RemoteBusinessVerificationRequest request, RemoteVerificationAttemptContext context)
      throws RemoteBusinessVerificationException;
}
