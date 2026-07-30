package com.reserly.platform.billing.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.payment.redsys.InvalidPaymentCallbackException;
import com.reserly.platform.billing.payment.redsys.RedsysCallbackVerificationService;
import com.reserly.platform.billing.payment.redsys.RedsysSignedMessage;
import com.reserly.platform.billing.payment.redsys.VerifiedRedsysCallback;
import com.reserly.platform.billing.persistence.PaymentCallbackReceiptDao;
import com.reserly.platform.billing.persistence.PaymentDao;
import com.reserly.platform.billing.persistence.PaymentEntity;
import com.reserly.platform.billing.service.PaymentConfirmation;
import com.reserly.platform.billing.service.SubscriptionPaymentApplicationService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Acredita correlacion previa, insercion atomica y paridad entre RedSys y simulador. */
class PaymentCallbackProcessingServiceTests {

  private static final UUID PAYMENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
  private static final Instant NOW = Instant.parse("2026-07-30T10:00:00Z");

  @Test
  void appliesFirstVerifiedConfirmationAfterDurableReceipt() {
    Fixture fixture = fixture();
    when(fixture
            .receiptDao()
            .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);
    when(fixture.subscriptionService().applyConfirmedPayment(any())).thenReturn(true);

    PaymentCallbackProcessingResult result =
        fixture.service().processRedsysNotification(fixture.message());

    assertThat(result.status()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(result.duplicate()).isFalse();
    assertThat(result.subscriptionUpdated()).isTrue();
    verify(fixture.subscriptionService())
        .applyConfirmedPayment(new PaymentConfirmation(PAYMENT_ID, "redsys", "1234567890", NOW));
  }

  @Test
  void returnsDuplicateWithoutApplyingSubscriptionTwice() {
    Fixture fixture = fixture();
    when(fixture
            .receiptDao()
            .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(0);

    PaymentCallbackProcessingResult result =
        fixture.service().processRedsysNotification(fixture.message());

    assertThat(result.duplicate()).isTrue();
    verify(fixture.subscriptionService(), never()).applyConfirmedPayment(any());
  }

  @Test
  void treatsBrowserReturnAsReadOnlyInspection() {
    Fixture fixture = fixture();

    PaymentCallbackProcessingResult result =
        fixture.service().inspectRedsysReturn(fixture.message());

    assertThat(result.status()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(result.subscriptionUpdated()).isFalse();
    verify(fixture.receiptDao(), never())
        .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any());
    verify(fixture.subscriptionService(), never()).applyConfirmedPayment(any());
  }

  @Test
  void rejectsAmountMismatchBeforeWritingReceipt() {
    Fixture fixture = fixture();
    when(fixture.redsysVerifier().verify(fixture.message()))
        .thenReturn(
            new VerifiedRedsysCallback(
                PAYMENT_ID,
                "1234567890",
                new BigDecimal("30.00"),
                "0000",
                PaymentStatus.CONFIRMED,
                "a".repeat(64)));

    assertThatThrownBy(() -> fixture.service().processRedsysNotification(fixture.message()))
        .isInstanceOf(InvalidPaymentCallbackException.class);
    verify(fixture.receiptDao(), never())
        .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void appliesDeterministicSimulatorThroughSameReceiptAndSubscriptionService() {
    Fixture fixture = fixture();
    fixture.payment().setProvider("simulated");
    fixture.payment().setProviderOrderId("sim_confirmed_001");
    fixture.payment().setRequestPayloadHash("b".repeat(64));
    when(fixture
            .receiptDao()
            .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);
    when(fixture.subscriptionService().applyConfirmedPayment(any())).thenReturn(true);
    PaymentOrderResult providerResult =
        new PaymentOrderResult(
            "simulated",
            "sim_confirmed_001",
            PaymentStatus.CONFIRMED,
            null,
            "b".repeat(64),
            Map.of("simulation", true));

    PaymentCallbackProcessingResult result =
        fixture.service().processProviderResult(PAYMENT_ID, providerResult);

    assertThat(result.subscriptionUpdated()).isTrue();
    verify(fixture.subscriptionService())
        .applyConfirmedPayment(
            new PaymentConfirmation(PAYMENT_ID, "simulated", "sim_confirmed_001", NOW));
  }

  private Fixture fixture() {
    RedsysCallbackVerificationService verifier = mock(RedsysCallbackVerificationService.class);
    PaymentDao paymentDao = mock(PaymentDao.class);
    PaymentCallbackReceiptDao receiptDao = mock(PaymentCallbackReceiptDao.class);
    SubscriptionPaymentApplicationService subscriptionService =
        mock(SubscriptionPaymentApplicationService.class);
    RedsysSignedMessage message = new RedsysSignedMessage("HMAC_SHA512_V2", "encoded", "signature");
    VerifiedRedsysCallback callback =
        new VerifiedRedsysCallback(
            PAYMENT_ID,
            "1234567890",
            new BigDecimal("29.00"),
            "0000",
            PaymentStatus.CONFIRMED,
            "a".repeat(64));
    PaymentEntity payment = new PaymentEntity();
    payment.setId(PAYMENT_ID);
    payment.setProvider("redsys");
    payment.setProviderOrderId("1234567890");
    payment.setAmount(new BigDecimal("29.00"));
    payment.setCurrency("EUR");
    when(verifier.verify(message)).thenReturn(callback);
    when(paymentDao.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
    when(paymentDao.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
    var service =
        new PaymentCallbackProcessingServiceImpl(
            verifier,
            paymentDao,
            receiptDao,
            subscriptionService,
            Clock.fixed(NOW, ZoneOffset.UTC));
    return new Fixture(service, verifier, receiptDao, subscriptionService, payment, message);
  }

  private record Fixture(
      PaymentCallbackProcessingServiceImpl service,
      RedsysCallbackVerificationService redsysVerifier,
      PaymentCallbackReceiptDao receiptDao,
      SubscriptionPaymentApplicationService subscriptionService,
      PaymentEntity payment,
      RedsysSignedMessage message) {}
}
