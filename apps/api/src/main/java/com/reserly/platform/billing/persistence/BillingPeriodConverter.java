package com.reserly.platform.billing.persistence;

import com.reserly.platform.billing.BillingPeriod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Convierte la periodicidad al valor estable definido por la migración. */
@Converter
public class BillingPeriodConverter implements AttributeConverter<BillingPeriod, String> {

  @Override
  public String convertToDatabaseColumn(BillingPeriod attribute) {
    return attribute == null ? null : attribute.persistedValue();
  }

  @Override
  public BillingPeriod convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : BillingPeriod.fromPersistedValue(databaseValue);
  }
}
