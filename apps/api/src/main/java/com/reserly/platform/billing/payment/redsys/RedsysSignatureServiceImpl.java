package com.reserly.platform.billing.payment.redsys;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Implementa la firma vigente HMAC_SHA512_V2 publicada por RedSys.
 *
 * <p>La clave del comercio se normaliza a 16 bytes ASCII, el pedido se cifra con AES-CBC e IV cero,
 * y la representacion Base64 de esa clave diversificada alimenta HMAC-SHA-512.
 */
@Service
public class RedsysSignatureServiceImpl implements RedsysSignatureService {

  private static final int AES_KEY_BYTES = 16;
  private static final int MAX_PARAMETERS_LENGTH = 16_384;
  private static final int MAX_SIGNATURE_LENGTH = 256;

  @Override
  public String sign(String merchantParameters, String order, String signingKey) {
    validateInputs(merchantParameters, order, signingKey);
    try {
      byte[] operationKey = diversify(order, signingKey);
      String operationKeyBase64 = Base64.getEncoder().encodeToString(operationKey);
      Mac mac = Mac.getInstance("HmacSHA512");
      mac.init(
          new SecretKeySpec(operationKeyBase64.getBytes(StandardCharsets.US_ASCII), "HmacSHA512"));
      byte[] signature = mac.doFinal(merchantParameters.getBytes(StandardCharsets.US_ASCII));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("RedSys signature algorithm unavailable", exception);
    }
  }

  @Override
  public boolean verify(
      String merchantParameters, String order, String signingKey, String signature) {
    if (signature == null || signature.isBlank() || signature.length() > MAX_SIGNATURE_LENGTH) {
      return false;
    }
    try {
      byte[] expected = Base64.getUrlDecoder().decode(sign(merchantParameters, order, signingKey));
      byte[] received = Base64.getUrlDecoder().decode(signature.strip());
      return MessageDigest.isEqual(expected, received);
    } catch (IllegalArgumentException exception) {
      return false;
    }
  }

  private byte[] diversify(String order, String signingKey) throws GeneralSecurityException {
    byte[] key = normalizedKey(signingKey);
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(key, "AES"),
        new IvParameterSpec(new byte[AES_KEY_BYTES]));
    return cipher.doFinal(order.getBytes(StandardCharsets.US_ASCII));
  }

  private byte[] normalizedKey(String signingKey) {
    String value = signingKey.strip();
    byte[] source = value.getBytes(StandardCharsets.US_ASCII);
    byte[] normalized = new byte[AES_KEY_BYTES];
    System.arraycopy(source, 0, normalized, 0, Math.min(source.length, normalized.length));
    return normalized;
  }

  private void validateInputs(String parameters, String order, String signingKey) {
    if (parameters == null
        || parameters.isBlank()
        || parameters.length() > MAX_PARAMETERS_LENGTH
        || !ascii(parameters)
        || order == null
        || !order.matches("[A-Za-z0-9]{5,12}")
        || signingKey == null
        || signingKey.isBlank()
        || signingKey.length() > 128
        || !ascii(signingKey.strip())) {
      throw new IllegalArgumentException("Invalid RedSys signature input");
    }
  }

  private boolean ascii(String value) {
    return value.chars().allMatch(character -> character >= 0x20 && character <= 0x7e);
  }
}
