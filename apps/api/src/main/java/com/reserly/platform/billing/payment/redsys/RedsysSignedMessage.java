package com.reserly.platform.billing.payment.redsys;

/**
 * Tres campos firmados enviados por RedSys en retorno o notificacion.
 *
 * @param signatureVersion version criptografica declarada
 * @param merchantParameters payload Base64URL exacto
 * @param signature firma Base64URL
 */
public record RedsysSignedMessage(
    String signatureVersion, String merchantParameters, String signature) {

  private static final int MAX_PARAMETERS_LENGTH = 16_384;
  private static final int MAX_SIGNATURE_LENGTH = 256;

  /** Limita el cuerpo antes de decodificarlo o verificarlo. */
  public RedsysSignedMessage {
    if (!RedsysPaymentProvider.SIGNATURE_VERSION.equals(signatureVersion)
        || merchantParameters == null
        || merchantParameters.isBlank()
        || merchantParameters.length() > MAX_PARAMETERS_LENGTH
        || signature == null
        || signature.isBlank()
        || signature.length() > MAX_SIGNATURE_LENGTH) {
      throw new InvalidPaymentCallbackException();
    }
  }
}
