package com.reserly.platform.identity.service;

/**
 * Genera y transforma secretos opacos de un solo uso.
 *
 * <p>El secreto original solo puede viajar hacia el destinatario. PostgreSQL recibe exclusivamente
 * su SHA-256.
 */
public interface OneTimeTokenService {

  /** Genera un secreto CSPRNG Base64 URL-safe de 256 bits. */
  String generate();

  /** Calcula la huella SHA-256 hexadecimal de un secreto con formato válido. */
  String hash(String token);

  /** Comprueba longitud y alfabeto antes de cualquier consulta persistente. */
  boolean isValid(String token);
}
