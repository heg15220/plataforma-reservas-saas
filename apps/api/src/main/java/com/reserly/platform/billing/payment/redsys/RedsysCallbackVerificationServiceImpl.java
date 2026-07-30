package com.reserly.platform.billing.payment.redsys;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.configuration.RedsysProperties;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Valida mensajes HMAC_SHA512_V2 sin conservar el payload decodificado.
 *
 * <p>Los campos desconocidos se toleran para compatibilidad futura, pero solo se copian escalares y
 * nunca se devuelven. La firma se comprueba antes de correlacionar o mutar datos.
 */
@Service
public class RedsysCallbackVerificationServiceImpl implements RedsysCallbackVerificationService {

  private static final String EURO_NUMERIC_CODE = "978";

  private final RedsysProperties properties;
  private final RedsysSignatureService signatureService;
  private final ObjectMapper objectMapper;

  public RedsysCallbackVerificationServiceImpl(
      RedsysProperties properties,
      RedsysSignatureService signatureService,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.signatureService = signatureService;
    this.objectMapper = objectMapper;
  }

  @Override
  public VerifiedRedsysCallback verify(RedsysSignedMessage message) {
    if (!properties.configured()) {
      throw new InvalidPaymentCallbackException();
    }
    try {
      Map<String, String> parameters = decode(message.merchantParameters());
      String order = required(parameters, "DS_ORDER", "[A-Za-z0-9]{5,12}");
      if (!signatureService.verify(
          message.merchantParameters(),
          order,
          properties.signingKey().strip(),
          message.signature())) {
        throw new InvalidPaymentCallbackException();
      }
      requireExpected(parameters, "DS_MERCHANTCODE", properties.merchantCode().strip());
      requireExpected(parameters, "DS_TERMINAL", normalizedTerminal());
      requireExpected(parameters, "DS_CURRENCY", EURO_NUMERIC_CODE);
      requireExpected(parameters, "DS_TRANSACTIONTYPE", "0");
      String amountMinor = required(parameters, "DS_AMOUNT", "[0-9]{1,12}");
      String responseCode = required(parameters, "DS_RESPONSE", "[0-9]{1,4}");
      UUID paymentId = UUID.fromString(required(parameters, "DS_MERCHANTDATA", "[0-9a-fA-F-]{36}"));
      return new VerifiedRedsysCallback(
          paymentId,
          order,
          new BigDecimal(amountMinor).movePointLeft(2).setScale(2),
          responseCode,
          outcome(responseCode),
          sha256(message.merchantParameters()));
    } catch (InvalidPaymentCallbackException exception) {
      throw exception;
    } catch (IllegalArgumentException | IOException exception) {
      throw new InvalidPaymentCallbackException(exception);
    }
  }

  private Map<String, String> decode(String encoded) throws IOException {
    byte[] json = Base64.getUrlDecoder().decode(encoded);
    if (json.length > 12_288) {
      throw new InvalidPaymentCallbackException();
    }
    JsonNode root = objectMapper.readTree(json);
    if (root == null || !root.isObject()) {
      throw new InvalidPaymentCallbackException();
    }
    Map<String, String> values = new LinkedHashMap<>();
    for (Map.Entry<String, JsonNode> field : root.properties()) {
      if (field.getValue().isValueNode()) {
        values.put(field.getKey().toUpperCase(Locale.ROOT), field.getValue().asText());
      }
    }
    return values;
  }

  private String required(Map<String, String> parameters, String key, String pattern) {
    String value = parameters.get(key);
    if (value == null || !value.matches(pattern)) {
      throw new InvalidPaymentCallbackException();
    }
    return value;
  }

  private void requireExpected(Map<String, String> parameters, String key, String expected) {
    if (!expected.equals(parameters.get(key))) {
      throw new InvalidPaymentCallbackException();
    }
  }

  private String normalizedTerminal() {
    return String.format("%03d", Integer.parseInt(properties.terminal().strip()));
  }

  private PaymentStatus outcome(String responseCode) {
    int response = Integer.parseInt(responseCode);
    if (response >= 0 && response <= 99) {
      return PaymentStatus.CONFIRMED;
    }
    if (response == 9915) {
      return PaymentStatus.CANCELLED_BY_USER;
    }
    if (response == 9997 || response == 9998 || response == 9999) {
      return PaymentStatus.PENDING_CONFIRMATION;
    }
    if (response == 909 || response == 912) {
      return PaymentStatus.COMMUNICATION_ERROR;
    }
    return PaymentStatus.REJECTED;
  }

  private String sha256(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(StandardCharsets.US_ASCII)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
