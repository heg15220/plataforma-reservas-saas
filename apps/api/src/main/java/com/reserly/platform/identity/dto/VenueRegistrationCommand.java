package com.reserly.platform.identity.dto;

/**
 * Comando interno de registro ya separado del contrato HTTP.
 *
 * <p>La contraseña solo vive durante la ejecución de la transacción y nunca debe incluirse en logs,
 * excepciones o respuestas.
 */
public record VenueRegistrationCommand(
    String email,
    String rawPassword,
    String preferredLocale,
    String taxCountry,
    String businessLegalName,
    String businessTaxIdentifier,
    String businessAddress,
    boolean acceptsLegalTerms) {}
