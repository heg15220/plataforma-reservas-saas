package com.reserly.platform.billing.payment.redsys;

/** Verifica firma y extrae exclusivamente los campos necesarios para correlacionar un pago. */
public interface RedsysCallbackVerificationService {

  /**
   * Verifica y normaliza un mensaje firmado.
   *
   * @throws InvalidPaymentCallbackException ante cualquier firma, formato o comercio inesperado
   */
  VerifiedRedsysCallback verify(RedsysSignedMessage message);
}
