package com.reserly.platform.forms.service;

/** Motivos internos estables que permiten traducir errores sin exponer detalles de persistencia. */
public enum ReservationFormResponseViolation {
  INVALID_SCHEMA,
  UNKNOWN_FIELD,
  DUPLICATE_FIELD,
  MISSING_REQUIRED,
  INVALID_TYPE,
  INVALID_VALUE
}
