package com.reserly.platform.businessverification.validation;

/**
 * Identidad fiscal canónica obtenida antes de persistir o consultar un proveedor remoto.
 *
 * @param taxCountry país fiscal ISO alpha-2 en mayúsculas
 * @param value identificador sin separadores de presentación ni prefijo de país redundante
 * @param scheme familia sintáctica reconocida
 * @param formatValidated indica que existe una regla local de formato para el país
 * @param controlCharacterValidated indica que se comprobó localmente el carácter de control
 */
public record NormalizedBusinessTaxIdentifier(
    String taxCountry,
    String value,
    BusinessTaxIdentifierScheme scheme,
    boolean formatValidated,
    boolean controlCharacterValidated) {}
