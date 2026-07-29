package com.reserly.platform.billing.payment;

/**
 * Puerto sustituible para crear órdenes externas.
 *
 * <p>El llamador debe persistir idempotencia y estados en una transacción separada de cualquier
 * I/O. Un adaptador no actualiza suscripciones ni pagos directamente.
 */
public interface PaymentProvider {

  /** Código corto almacenado en {@code Payments.provider}. */
  String providerCode();

  /**
   * Crea o reproduce de forma idempotente una orden.
   *
   * @throws PaymentProviderUnavailableException cuando el proveedor no está habilitado
   */
  PaymentOrderResult createOrder(PaymentOrderCommand command);
}
