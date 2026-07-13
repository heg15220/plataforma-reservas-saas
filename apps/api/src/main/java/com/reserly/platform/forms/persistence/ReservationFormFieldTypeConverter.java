package com.reserly.platform.forms.persistence;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Conserva en PostgreSQL los codigos snake_case del contrato, no los nombres Java del enum. */
@Converter
public class ReservationFormFieldTypeConverter
    implements AttributeConverter<ReservationFormFieldType, String> {

  @Override
  public String convertToDatabaseColumn(ReservationFormFieldType attribute) {
    return attribute == null ? null : attribute.code();
  }

  @Override
  public ReservationFormFieldType convertToEntityAttribute(String value) {
    return ReservationFormFieldType.fromCode(value)
        .orElseThrow(() -> new IllegalArgumentException("Unknown reservation form field type"));
  }
}
