package com.reserly.platform.billing.payment.redsys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.configuration.RedsysProperties;
import java.net.URI;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Verifica firma antes de interpretar resultado y minimiza campos de respuesta. */
class RedsysCallbackVerificationServiceTests {

  private static final String KEY = "sq7HjrUOBfKmC576ILgskD5srU870gJ7";
  private static final UUID PAYMENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final RedsysSignatureService signatureService = new RedsysSignatureServiceImpl();
  private final RedsysCallbackVerificationService service =
      new RedsysCallbackVerificationServiceImpl(
          new RedsysProperties(
              URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"),
              "999008881",
              "001",
              KEY),
          signatureService,
          objectMapper);

  @Test
  void verifiesAndNormalizesAuthorizedResponseWhileIgnoringUnknownSensitiveFields()
      throws Exception {
    RedsysSignedMessage message =
        signedMessage(
            "0000",
            Map.of(
                "Ds_Control_NEW_FIELD", "future",
                "Ds_Card_Number", "masked-value-not-returned"));

    VerifiedRedsysCallback callback = service.verify(message);

    assertThat(callback.paymentId()).isEqualTo(PAYMENT_ID);
    assertThat(callback.order()).isEqualTo("1234567890");
    assertThat(callback.amount()).isEqualByComparingTo("29.00");
    assertThat(callback.status()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(callback.payloadHash()).matches("[0-9a-f]{64}");
    assertThat(callback.toString()).doesNotContain("future", "masked-value", "signature");
  }

  @Test
  void mapsCancellationPendingCommunicationAndRejectionCodes() throws Exception {
    assertThat(service.verify(signedMessage("9915", Map.of())).status())
        .isEqualTo(PaymentStatus.CANCELLED_BY_USER);
    assertThat(service.verify(signedMessage("9999", Map.of())).status())
        .isEqualTo(PaymentStatus.PENDING_CONFIRMATION);
    assertThat(service.verify(signedMessage("0912", Map.of())).status())
        .isEqualTo(PaymentStatus.COMMUNICATION_ERROR);
    assertThat(service.verify(signedMessage("0190", Map.of())).status())
        .isEqualTo(PaymentStatus.REJECTED);
  }

  @Test
  void rejectsTamperedSignatureAndUnexpectedMerchantBeforeCorrelation() throws Exception {
    RedsysSignedMessage valid = signedMessage("0000", Map.of());
    assertThatThrownBy(
            () ->
                service.verify(
                    new RedsysSignedMessage(
                        valid.signatureVersion(),
                        valid.merchantParameters(),
                        valid.signature() + "A")))
        .isInstanceOf(InvalidPaymentCallbackException.class);

    RedsysSignedMessage wrongMerchant =
        signedMessage("0000", Map.of("Ds_MerchantCode", "111111111"));
    assertThatThrownBy(() -> service.verify(wrongMerchant))
        .isInstanceOf(InvalidPaymentCallbackException.class);
  }

  private RedsysSignedMessage signedMessage(String response, Map<String, String> overrides)
      throws Exception {
    Map<String, String> values = new LinkedHashMap<>();
    values.put("Ds_Order", "1234567890");
    values.put("Ds_Amount", "2900");
    values.put("Ds_Currency", "978");
    values.put("Ds_Response", response);
    values.put("Ds_MerchantCode", "999008881");
    values.put("Ds_Terminal", "001");
    values.put("Ds_TransactionType", "0");
    values.put("Ds_MerchantData", PAYMENT_ID.toString());
    values.putAll(overrides);
    String encoded =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(objectMapper.writeValueAsBytes(values));
    String signature = signatureService.sign(encoded, "1234567890", KEY);
    return new RedsysSignedMessage("HMAC_SHA512_V2", encoded, signature);
  }
}
