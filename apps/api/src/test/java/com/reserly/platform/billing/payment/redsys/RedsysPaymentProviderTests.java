package com.reserly.platform.billing.payment.redsys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.payment.PaymentOrderCommand;
import com.reserly.platform.billing.payment.PaymentProviderUnavailableException;
import com.reserly.platform.configuration.RedsysProperties;
import com.reserly.platform.configuration.ReserlyEnvironment;
import com.reserly.platform.configuration.ReserlyProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica el contrato de redireccion sin red, tarjeta ni persistencia de firma. */
class RedsysPaymentProviderTests {

  private static final String KEY = "sq7HjrUOBfKmC576ILgskD5srU870gJ7";
  private static final UUID PAYMENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RedsysSignatureService signatureService = new RedsysSignatureServiceImpl();

  @Test
  void buildsSignedPostFormWithExactOfficialFieldsAndMinorUnits() throws Exception {
    RedsysPaymentProvider provider =
        new RedsysPaymentProvider(
            configuredProperties(), platformProperties(), signatureService, objectMapper);

    var result = provider.createOrder(command("1234567890"));

    assertThat(result.provider()).isEqualTo("redsys");
    assertThat(result.status()).isEqualTo(PaymentStatus.PENDING_CONFIRMATION);
    assertThat(result.redirect().action())
        .isEqualTo(URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"));
    assertThat(result.redirect().fields())
        .containsKeys("Ds_MerchantParameters", "Ds_SignatureVersion", "Ds_Signature");
    assertThat(result.redirect().fields().get("Ds_SignatureVersion")).isEqualTo("HMAC_SHA512_V2");

    String encoded = result.redirect().fields().get("Ds_MerchantParameters");
    Map<String, String> parameters =
        objectMapper.readValue(
            Base64.getUrlDecoder().decode(encoded), new TypeReference<Map<String, String>>() {});
    assertThat(parameters)
        .containsEntry("DS_MERCHANT_ORDER", "1234567890")
        .containsEntry("DS_MERCHANT_AMOUNT", "2900")
        .containsEntry("DS_MERCHANT_CURRENCY", "978")
        .containsEntry("DS_MERCHANT_MERCHANTDATA", PAYMENT_ID.toString())
        .containsEntry(
            "DS_MERCHANT_MERCHANTURL",
            "https://api.example.invalid/api/payments/redsys/notification");
    assertThat(
            signatureService.verify(
                encoded, "1234567890", KEY, result.redirect().fields().get("Ds_Signature")))
        .isTrue();
    assertThat(result.requestPayloadHash()).matches("[0-9a-f]{64}");
    assertThat(result.responsePayload().toString())
        .doesNotContain("Ds_Signature", KEY, "card", "pan", "cvv");
  }

  @Test
  void failsClosedWithoutCompleteCredentialsAndRejectsNonRedsysOrder() {
    RedsysPaymentProvider unavailable =
        new RedsysPaymentProvider(
            new RedsysProperties(
                URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"), "", "", ""),
            platformProperties(),
            signatureService,
            objectMapper);
    assertThatThrownBy(() -> unavailable.createOrder(command("1234567890")))
        .isInstanceOf(PaymentProviderUnavailableException.class);

    RedsysPaymentProvider configured =
        new RedsysPaymentProvider(
            configuredProperties(), platformProperties(), signatureService, objectMapper);
    assertThatThrownBy(() -> configured.createOrder(command("sim_confirmed_001")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private PaymentOrderCommand command(String order) {
    return new PaymentOrderCommand(
        PAYMENT_ID,
        UUID.fromString("10000000-0000-4000-8000-000000000001"),
        UUID.fromString("20000000-0000-4000-8000-000000000001"),
        order,
        new BigDecimal("29.00"),
        "EUR");
  }

  private RedsysProperties configuredProperties() {
    return new RedsysProperties(
        URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"), "999008881", "1", KEY);
  }

  private ReserlyProperties platformProperties() {
    URI api = URI.create("https://api.example.invalid");
    URI web = URI.create("https://web.example.invalid");
    return new ReserlyProperties(
        ReserlyEnvironment.STAGING,
        api,
        web,
        List.of(web),
        new ReserlyProperties.Security(true),
        new ReserlyProperties.Features(false));
  }
}
