package com.reserly.platform.billing.payment.redsys;

/** El callback no supera firma, formato o correlacion con el pago local. */
public class InvalidPaymentCallbackException extends RuntimeException {

  public InvalidPaymentCallbackException() {}

  public InvalidPaymentCallbackException(Throwable cause) {
    super(null, cause, false, false);
  }
}
