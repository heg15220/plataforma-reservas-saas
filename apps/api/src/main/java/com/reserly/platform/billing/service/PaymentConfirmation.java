package com.reserly.platform.billing.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Evidencia minima de un resultado confirmado por un adaptador ya verificado.
 *
 * @param paymentId pago local correlacionado
 * @param provider proveedor persistido
 * @param providerOrderId pedido dentro del proveedor
 * @param confirmedAt instante de procesamiento servidor
 */
public record PaymentConfirmation(
    UUID paymentId, String provider, String providerOrderId, Instant confirmedAt) {

  /** Rechaza referencias incompletas antes de adquirir locks. */
  public PaymentConfirmation {
    if (paymentId == null
        || provider == null
        || provider.isBlank()
        || providerOrderId == null
        || providerOrderId.isBlank()
        || confirmedAt == null) {
      throw new IllegalArgumentException("Invalid payment confirmation");
    }
  }
}
