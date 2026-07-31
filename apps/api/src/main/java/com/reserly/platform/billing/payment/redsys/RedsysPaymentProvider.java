package com.reserly.platform.billing.payment.redsys;

import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.payment.PaymentOrderCommand;
import com.reserly.platform.billing.payment.PaymentOrderResult;
import com.reserly.platform.billing.payment.PaymentProvider;
import com.reserly.platform.billing.payment.PaymentProviderUnavailableException;
import com.reserly.platform.billing.payment.PaymentRedirect;
import com.reserly.platform.configuration.RedsysProperties;
import com.reserly.platform.configuration.ReserlyProperties;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Construye el formulario de redireccion RedSys sin realizar I/O.
 *
 * <p>El navegador publica directamente los campos firmados en RedSys. El adaptador no solicita,
 * recibe ni almacena PAN, CVV o datos de autenticacion de tarjeta.
 */
public final class RedsysPaymentProvider implements PaymentProvider {

  public static final String PROVIDER_CODE = "redsys";
  public static final String SIGNATURE_VERSION = "HMAC_SHA512_V2";
  private static final String EURO_NUMERIC_CODE = "978";

  private final RedsysProperties redsys;
  private final ReserlyProperties platform;
  private final RedsysSignatureService signatureService;
  private final ObjectMapper objectMapper;

  public RedsysPaymentProvider(
      RedsysProperties redsys,
      ReserlyProperties platform,
      RedsysSignatureService signatureService,
      ObjectMapper objectMapper) {
    this.redsys = redsys;
    this.platform = platform;
    this.signatureService = signatureService;
    this.objectMapper = objectMapper;
  }

  @Override
  public String providerCode() {
    return PROVIDER_CODE;
  }

  @Override
  public PaymentOrderResult createOrder(PaymentOrderCommand command) {
    requireConfigured();
    if (!command.merchantOrderId().matches("[A-Za-z0-9]{5,12}")
        || !"EUR".equals(command.currency())) {
      throw new IllegalArgumentException("Invalid RedSys order");
    }

    String merchantParameters = encodeParameters(command);
    String signature =
        signatureService.sign(
            merchantParameters, command.merchantOrderId(), redsys.signingKey().strip());
    Map<String, String> fields =
        Map.of(
            "Ds_MerchantParameters",
            merchantParameters,
            "Ds_SignatureVersion",
            SIGNATURE_VERSION,
            "Ds_Signature",
            signature);
    return new PaymentOrderResult(
        PROVIDER_CODE,
        command.merchantOrderId(),
        PaymentStatus.PENDING_CONFIRMATION,
        new PaymentRedirect(redsys.paymentEndpoint(), fields),
        sha256(merchantParameters),
        Map.of("integration", "redirect", "signatureVersion", SIGNATURE_VERSION));
  }

  private String encodeParameters(PaymentOrderCommand command) {
    Map<String, String> parameters = new LinkedHashMap<>();
    parameters.put("DS_MERCHANT_ORDER", command.merchantOrderId());
    parameters.put("DS_MERCHANT_MERCHANTCODE", redsys.merchantCode().strip());
    parameters.put("DS_MERCHANT_TERMINAL", normalizedTerminal());
    parameters.put("DS_MERCHANT_CURRENCY", EURO_NUMERIC_CODE);
    parameters.put("DS_MERCHANT_TRANSACTIONTYPE", "0");
    parameters.put("DS_MERCHANT_AMOUNT", minorUnits(command));
    parameters.put(
        "DS_MERCHANT_MERCHANTURL",
        publicUri(platform.apiPublicBaseUrl(), "/api/payments/redsys/notification").toString());
    parameters.put(
        "DS_MERCHANT_URLOK",
        publicUri(platform.webPublicBaseUrl(), "/panel/suscripcion?payment=ok").toString());
    parameters.put(
        "DS_MERCHANT_URLKO",
        publicUri(platform.webPublicBaseUrl(), "/panel/suscripcion?payment=ko").toString());
    parameters.put("DS_MERCHANT_MERCHANTDATA", command.paymentId().toString());
    try {
      byte[] json = objectMapper.writeValueAsBytes(parameters);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Unable to encode RedSys order", exception);
    }
  }

  private String normalizedTerminal() {
    return String.format("%03d", Integer.parseInt(redsys.terminal().strip()));
  }

  private String minorUnits(PaymentOrderCommand command) {
    BigInteger value = command.amount().movePointRight(2).toBigIntegerExact();
    if (value.signum() <= 0 || value.toString().length() > 12) {
      throw new IllegalArgumentException("Invalid RedSys amount");
    }
    return value.toString();
  }

  private URI publicUri(URI base, String path) {
    return base.resolve(path);
  }

  private void requireConfigured() {
    if (!redsys.configured()) {
      throw new PaymentProviderUnavailableException();
    }
  }

  private String sha256(String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
