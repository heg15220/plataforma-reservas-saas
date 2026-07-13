package com.reserly.platform.forms.persistence;

import java.util.Arrays;
import java.util.Optional;

/** Tipos persistidos y expuestos por los campos personalizados del formulario de reserva. */
public enum ReservationFormFieldType {
  SHORT_TEXT("short_text"),
  LONG_TEXT("long_text"),
  NUMBER("number"),
  SELECT("select"),
  CHECKBOX("checkbox"),
  DATE("date"),
  PHONE("phone"),
  EMAIL("email");

  private final String code;

  ReservationFormFieldType(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  /** Resuelve exclusivamente los codigos publicos exactos definidos por RF-013. */
  public static Optional<ReservationFormFieldType> fromCode(String code) {
    return Arrays.stream(values()).filter(type -> type.code.equals(code)).findFirst();
  }
}
