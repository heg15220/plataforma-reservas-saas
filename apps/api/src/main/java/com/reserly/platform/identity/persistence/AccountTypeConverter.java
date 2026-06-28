package com.reserly.platform.identity.persistence;

import com.reserly.platform.identity.AccountType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Traduce {@link AccountType} a su representación canónica en minúsculas.
 *
 * <p>La conversión estricta impide aceptar silenciosamente valores desconocidos si el catálogo de
 * aplicación y el esquema divergen.
 */
@Converter
public class AccountTypeConverter implements AttributeConverter<AccountType, String> {

  /** Convierte el tipo de dominio al valor definido por el contrato SQL. */
  @Override
  public String convertToDatabaseColumn(AccountType attribute) {
    return attribute == null ? null : attribute.persistedValue();
  }

  /** Convierte el valor SQL al enum y falla ante catálogos incompatibles. */
  @Override
  public AccountType convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : AccountType.fromPersistedValue(databaseValue);
  }
}
