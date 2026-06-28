package com.reserly.platform.businessverification.validation;

/**
 * Frontera de dominio para obtener una identidad fiscal canónica y aplicar reglas locales.
 *
 * <p>Los países con estrategia registrada validan formato y carácter de control. El resto usa una
 * normalización conservadora para poder continuar en estado no verificado hasta que exista un
 * adaptador específico.
 */
public interface BusinessTaxIdentifierValidationService {

  /**
   * Normaliza país e identificador y aplica la regla local disponible.
   *
   * @throws BusinessTaxIdentifierValidationException si el valor no es seguro o incumple una regla
   *     conocida
   */
  NormalizedBusinessTaxIdentifier normalizeAndValidate(
      String taxCountry, String businessTaxIdentifier);
}
