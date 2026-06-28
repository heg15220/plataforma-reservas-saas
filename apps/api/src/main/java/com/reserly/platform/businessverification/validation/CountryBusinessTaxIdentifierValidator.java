package com.reserly.platform.businessverification.validation;

/**
 * Estrategia local de normalización y validación para un país con reglas conocidas.
 *
 * <p>Las implementaciones no realizan llamadas de red. Una validación local correcta solo habilita
 * la posterior comprobación remota o administrativa.
 */
public interface CountryBusinessTaxIdentifierValidator {

  /** Código ISO alpha-2 exacto gestionado por la estrategia. */
  String supportedCountry();

  /**
   * Valida un valor ya compactado a caracteres ASCII alfanuméricos.
   *
   * @throws BusinessTaxIdentifierValidationException si formato o control son inválidos
   */
  NormalizedBusinessTaxIdentifier validate(String compactIdentifier);
}
