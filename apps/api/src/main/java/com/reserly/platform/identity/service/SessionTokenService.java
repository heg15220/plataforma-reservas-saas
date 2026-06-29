package com.reserly.platform.identity.service;

/** Genera y deriva secretos opacos de sesión sin persistir el valor entregado al navegador. */
public interface SessionTokenService {

  /** Devuelve 256 bits aleatorios codificados como Base64 URL-safe sin padding. */
  String generate();

  /** Calcula SHA-256 hexadecimal en minúsculas sobre un token validado. */
  String hash(String token);

  /** Valida únicamente el formato público acotado antes de hashear una cookie recibida. */
  boolean isValid(String token);
}
