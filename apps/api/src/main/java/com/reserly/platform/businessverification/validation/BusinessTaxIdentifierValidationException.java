package com.reserly.platform.businessverification.validation;

/**
 * Indica que un país o identificador no puede convertirse en una identidad fiscal canónica segura.
 *
 * <p>La excepción no incluye el identificador recibido para evitar que datos fiscales terminen en
 * logs o respuestas públicas.
 */
public class BusinessTaxIdentifierValidationException extends RuntimeException {

  public BusinessTaxIdentifierValidationException() {
    super("Business tax identifier is not locally valid");
  }
}
