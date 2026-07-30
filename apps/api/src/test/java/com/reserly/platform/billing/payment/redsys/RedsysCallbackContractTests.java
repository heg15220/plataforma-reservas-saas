package com.reserly.platform.billing.payment.redsys;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.payment.PaymentCallbackProcessingServiceImpl;
import com.reserly.platform.billing.persistence.PaymentCallbackReceiptDao;
import com.reserly.platform.billing.persistence.PaymentDao;
import com.reserly.platform.billing.persistence.PaymentEntity;
import com.reserly.platform.billing.service.SubscriptionPaymentApplicationService;
import com.reserly.platform.configuration.RedsysProperties;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Prueba el contrato completo firma-verificación-correlación-idempotencia sin red ni base real.
 */
class RedsysCallbackContractTests {

  private static final UUID PAYMENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
  private static final Instant NOW = Instant.parse("2026-07-30T12:00:00Z");
  private static final String ORDER = "1234567890";
  private static final String KEY = "sq7HjrUOBfKmC576ILgskD5srU870gJ7";

  @Test
  void acceptsAuthenticNotificationAndAppliesAnExactRetryOnlyOnce() throws Exception {
    Fixture fixture = fixture();
    when(fixture
            .receiptDao()
            .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1, 0);
    when(fixture.subscriptionService().applyConfirmedPayment(any())).thenReturn(true);

    var first = fixture.service().processRedsysNotification(fixture.message());
    var retry = fixture.service().processRedsysNotification(fixture.message());

    assertThat(first.status()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(first.subscriptionUpdated()).isTrue();
    assertThat(retry.duplicate()).isTrue();
    assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(fixture.payment().getPaidAt()).isEqualTo(NOW);
    assertThat(fixture.payment().getResponsePayloadJson())
        .containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "channel", "notification",
                "outcome", "confirmed",
                "providerResponseCode", "0000"));
    verify(fixture.paymentDao()).saveAndFlush(fixture.payment());
    verify(fixture.subscriptionService()).applyConfirmedPayment(any());
    verify(fixture.receiptDao(), times(2))
        .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void rejectsTamperingBeforeReadingOrWritingPaymentState() throws Exception {
    Fixture fixture = fixture();
    RedsysSignedMessage tampered =
        new RedsysSignedMessage(
            fixture.message().signatureVersion(),
            fixture.message().merchantParameters() + "A",
            fixture.message().signature());

    assertThatThrownBy(() -> fixture.service().processRedsysNotification(tampered))
        .isInstanceOf(InvalidPaymentCallbackException.class);
    verifyNoInteractions(
        fixture.paymentDao(), fixture.receiptDao(), fixture.subscriptionService());
  }

  private Fixture fixture() throws Exception {
    RedsysProperties properties =
        new RedsysProperties(
            URI.create("https://sis-t.redsys.es:25443/sis/realizarPago"),
            "999008881",
            "1",
            KEY);
    RedsysSignatureService signer = new RedsysSignatureServiceImpl();
    ObjectMapper objectMapper = new ObjectMapper();
    RedsysCallbackVerificationService verifier =
        new RedsysCallbackVerificationServiceImpl(properties, signer, objectMapper);
    PaymentDao paymentDao = mock(PaymentDao.class);
    PaymentCallbackReceiptDao receiptDao = mock(PaymentCallbackReceiptDao.class);
    SubscriptionPaymentApplicationService subscriptionService =
        mock(SubscriptionPaymentApplicationService.class);
    PaymentEntity payment = payment();
    when(paymentDao.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
    String parameters = parameters(objectMapper);
    RedsysSignedMessage message =
        new RedsysSignedMessage(
            RedsysPaymentProvider.SIGNATURE_VERSION,
            parameters,
            signer.sign(parameters, ORDER, KEY));
    var service =
        new PaymentCallbackProcessingServiceImpl(
            verifier,
            paymentDao,
            receiptDao,
            subscriptionService,
            Clock.fixed(NOW, ZoneOffset.UTC));
    return new Fixture(
        service, paymentDao, receiptDao, subscriptionService, payment, message);
  }

  private PaymentEntity payment() {
    PaymentEntity payment = new PaymentEntity();
    payment.setId(PAYMENT_ID);
    payment.setProvider(RedsysPaymentProvider.PROVIDER_CODE);
    payment.setProviderOrderId(ORDER);
    payment.setAmount(new BigDecimal("29.00"));
    payment.setCurrency("EUR");
    payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
    payment.setCreatedAt(NOW.minusSeconds(60));
    payment.setUpdatedAt(NOW.minusSeconds(60));
    return payment;
  }

  private String parameters(ObjectMapper objectMapper) throws Exception {
    Map<String, String> parameters = new LinkedHashMap<>();
    parameters.put("Ds_Order", ORDER);
    parameters.put("Ds_MerchantCode", "999008881");
    parameters.put("Ds_Terminal", "001");
    parameters.put("Ds_Currency", "978");
    parameters.put("Ds_TransactionType", "0");
    parameters.put("Ds_Amount", "2900");
    parameters.put("Ds_Response", "0000");
    parameters.put("Ds_MerchantData", PAYMENT_ID.toString());
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(
            objectMapper.writeValueAsString(parameters).getBytes(StandardCharsets.UTF_8));
  }

  private record Fixture(
      PaymentCallbackProcessingServiceImpl service,
      PaymentDao paymentDao,
      PaymentCallbackReceiptDao receiptDao,
      SubscriptionPaymentApplicationService subscriptionService,
      PaymentEntity payment,
      RedsysSignedMessage message) {}
}
