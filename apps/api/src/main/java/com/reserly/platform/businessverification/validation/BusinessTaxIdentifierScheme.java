package com.reserly.platform.businessverification.validation;

/**
 * Familias de identificadores reconocidas por la validación fiscal local.
 *
 * <p>El esquema describe la sintaxis comprobada, no acredita que la identidad exista ni que esté
 * dada de alta para operaciones intracomunitarias.
 */
public enum BusinessTaxIdentifierScheme {
  SPAIN_DNI_NIF,
  SPAIN_NIE,
  SPAIN_SPECIAL_PERSON_NIF,
  SPAIN_ENTITY_NIF,
  GENERIC
}
