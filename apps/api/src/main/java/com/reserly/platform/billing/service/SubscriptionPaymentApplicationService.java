package com.reserly.platform.billing.service;

/**
 * Aplica una confirmacion correlacionada a la suscripcion dentro de una transaccion corta.
 *
 * <p>El servicio no verifica firmas; solo acepta evidencia creada por el procesador del proveedor.
 */
public interface SubscriptionPaymentApplicationService {

  /**
   * Activa o renueva el periodo una sola vez por pago.
   *
   * @return {@code true} si cambio la suscripcion o {@code false} si el pago ya estaba aplicado
   */
  boolean applyConfirmedPayment(PaymentConfirmation confirmation);
}
