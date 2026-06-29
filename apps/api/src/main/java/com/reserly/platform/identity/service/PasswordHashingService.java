package com.reserly.platform.identity.service;

/**
 * Protege contraseñas antes de cualquier persistencia.
 *
 * <p>Centraliza el límite de entrada de BCrypt, generación con sal, verificación fail-closed y
 * detección de actualización. Ningún consumidor debe usar el encoder directamente.
 */
public interface PasswordHashingService {

  /**
   * Valida únicamente invariantes criptográficas de entrada.
   *
   * <p>Longitud mínima y complejidad pertenecen a cada caso de uso; aquí se rechazan valores nulos,
   * vacíos o superiores a 72 bytes UTF-8 para impedir truncamiento BCrypt.
   *
   * @throws PasswordHashingValidationException si la entrada no puede protegerse sin pérdida
   */
  void validate(String rawPassword);

  /**
   * Genera un hash adaptativo con sal aleatoria.
   *
   * @param rawPassword contraseña validada en claro; no debe registrarse ni conservarse
   * @return hash autocontenido apto para persistencia
   */
  String hash(String rawPassword);

  /**
   * Compara una contraseña sin lanzar ante entrada o hash malformado.
   *
   * <p>Cuando el hash no existe o no es BCrypt válido realiza una comparación contra un hash dummy
   * para reducir diferencias temporales en el futuro login.
   */
  boolean matches(String rawPassword, String encodedHash);

  /**
   * Indica si un hash válido debe regenerarse tras autenticar correctamente.
   *
   * <p>Se actualiza si usa una variante anterior o un coste inferior. Un hash malformado también
   * devuelve {@code true}, aunque nunca debe rehashearse sin verificar antes la contraseña.
   */
  boolean requiresRehash(String encodedHash);
}
