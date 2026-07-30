package com.reserly.platform.billing.payment.redsys;

/**
 * Contrato criptografico HMAC_SHA512_V2 de RedSys.
 *
 * <p>El valor firmado es siempre {@code Ds_MerchantParameters} codificado en Base64URL, no el JSON
 * decodificado.
 */
public interface RedsysSignatureService {

  /**
   * Firma parametros usando una clave diversificada con el numero de pedido.
   *
   * @param merchantParameters parametros Base64URL exactos
   * @param order numero de pedido contenido en los parametros
   * @param signingKey clave secreta del comercio
   * @return firma Base64URL sin padding
   */
  String sign(String merchantParameters, String order, String signingKey);

  /** Compara en tiempo constante una firma recibida con la firma calculada. */
  boolean verify(String merchantParameters, String order, String signingKey, String signature);
}
