package com.reserly.platform.billing.payment;

/**
 * Adaptador de fallo cerrado usado en producción mientras RedSys real no esté aprobado.
 *
 * <p>Evita un bean ausente y hace explícito que ninguna orden puede salir del sistema.
 */
public final class DisabledPaymentProvider implements PaymentProvider {

  @Override
  public String providerCode() {
    return "disabled";
  }

  @Override
  public PaymentOrderResult createOrder(PaymentOrderCommand command) {
    throw new PaymentProviderUnavailableException();
  }
}
