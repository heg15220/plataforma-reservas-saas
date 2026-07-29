package com.reserly.platform.billing.persistence;

import com.reserly.platform.billing.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Convierte el resultado de pago al código externo estable. */
@Converter
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

  @Override
  public String convertToDatabaseColumn(PaymentStatus attribute) {
    return attribute == null ? null : attribute.persistedValue();
  }

  @Override
  public PaymentStatus convertToEntityAttribute(String databaseValue) {
    return databaseValue == null ? null : PaymentStatus.fromPersistedValue(databaseValue);
  }
}
