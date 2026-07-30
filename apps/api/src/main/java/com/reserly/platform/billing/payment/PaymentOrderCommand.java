package com.reserly.platform.billing.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Orden neutral respecto al proveedor.
 *
 * @param paymentId intento local que se correlacionara sin exponer la suscripción
 * @param subscriptionId suscripción objetivo
 * @param venueId local propietario
 * @param merchantOrderId identificador idempotente generado por Reserly
 * @param amount importe positivo con máximo dos decimales
 * @param currency moneda ISO de tres letras
 */
public record PaymentOrderCommand(
    UUID paymentId,
    UUID subscriptionId,
    UUID venueId,
    String merchantOrderId,
    BigDecimal amount,
    String currency) {

  /** Normaliza moneda e importe antes de que un adaptador calcule firma o hash. */
  public PaymentOrderCommand {
    Objects.requireNonNull(paymentId, "paymentId");
    Objects.requireNonNull(subscriptionId, "subscriptionId");
    Objects.requireNonNull(venueId, "venueId");
    if (merchantOrderId == null || merchantOrderId.isBlank() || merchantOrderId.length() > 128) {
      throw new IllegalArgumentException("Invalid merchant order id");
    }
    merchantOrderId = merchantOrderId.strip();
    if (amount == null || amount.signum() <= 0) {
      throw new IllegalArgumentException("Invalid payment amount");
    }
    try {
      amount = amount.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("Invalid payment amount", exception);
    }
    if (currency == null || !currency.matches("[A-Za-z]{3}")) {
      throw new IllegalArgumentException("Invalid payment currency");
    }
    currency = currency.toUpperCase(Locale.ROOT);
  }
}
