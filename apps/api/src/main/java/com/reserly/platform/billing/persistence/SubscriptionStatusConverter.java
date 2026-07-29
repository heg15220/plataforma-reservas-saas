package com.reserly.platform.billing.persistence;

import com.reserly.platform.billing.SubscriptionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Convierte estados de suscripción sin depender del nombre Java del enum. */
@Converter
public class SubscriptionStatusConverter implements AttributeConverter<SubscriptionStatus, String> {

  @Override
  public String convertToDatabaseColumn(SubscriptionStatus attribute) {
    return attribute == null ? null : attribute.persistedValue();
  }

  @Override
  public SubscriptionStatus convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : SubscriptionStatus.fromPersistedValue(databaseValue);
  }
}
