package com.reserly.platform.identity.service;

/**
 * Protege contraseñas antes de cualquier persistencia.
 *
 * <p>Este contrato inicial solo permite generar hashes para registro. La tarea 1.12 ampliará la
 * política con verificación, configuración de coste, detección de rehash y pruebas específicas del
 * ciclo completo de credenciales.
 */
public interface PasswordHashingService {

  /**
   * Genera un hash adaptativo con sal aleatoria.
   *
   * @param rawPassword contraseña validada en claro; no debe registrarse ni conservarse
   * @return hash autocontenido apto para persistencia
   */
  String hash(String rawPassword);
}
