package com.reserly.platform.billing.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.administration.service.AuditLogService;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

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
    assertThat(fixture.payment().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(fixture.payment().getPaidAt()).isEqualTo(NOW);
    assertThat(fixture.payment().getResponsePayloadJson())
        .containsEntry("channel", "notification")
        .containsEntry("providerResponseCode", "0000")
        .doesNotContainKeys("signature", "merchantParameters");
    verify(fixture.paymentDao()).saveAndFlush(fixture.payment());
    verify(fixture.subscriptionService())
        .applyConfirmedPayment(new PaymentConfirmation(PAYMENT_ID, "redsys", "1234567890", NOW));
    verify(fixture.auditLogService()).record(any());
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
    verify(fixture.paymentDao(), never()).saveAndFlush(any());
    verify(fixture.subscriptionService(), never()).applyConfirmedPayment(any());
    verify(fixture.auditLogService(), never()).record(any());
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

  @ParameterizedTest
  @EnumSource(
      value = PaymentStatus.class,
      names = {"REJECTED", "CANCELLED_BY_USER", "COMMUNICATION_ERROR", "PENDING_CONFIRMATION"})
  void persistsEveryNonConfirmedOutcomeWithoutPaidDateOrSubscriptionChange(PaymentStatus outcome) {
    Fixture fixture = fixture();
    when(fixture.redsysVerifier().verify(fixture.message()))
        .thenReturn(
            new VerifiedRedsysCallback(
                PAYMENT_ID,
                "1234567890",
                new BigDecimal("29.00"),
                responseCode(outcome),
                outcome,
                outcome.name().substring(0, 1).toLowerCase().repeat(64)));
    when(fixture
            .receiptDao()
            .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    PaymentCallbackProcessingResult result =
        fixture.service().processRedsysNotification(fixture.message());

    assertThat(result.status()).isEqualTo(outcome);
    assertThat(fixture.payment().getStatus()).isEqualTo(outcome);
    assertThat(fixture.payment().getPaidAt()).isNull();
    assertThat(fixture.payment().getUpdatedAt()).isEqualTo(NOW);
    verify(fixture.paymentDao()).saveAndFlush(fixture.payment());
    verifyNoInteractions(fixture.subscriptionService());
  }

  @Test
  void neverDowngradesConfirmedPaymentWithALaterNonConfirmedCallback() {
    Fixture fixture = fixture();
    fixture.payment().setStatus(PaymentStatus.CONFIRMED);
    fixture.payment().setPaidAt(NOW.minusSeconds(60));
    when(fixture.redsysVerifier().verify(fixture.message()))
        .thenReturn(
            new VerifiedRedsysCallback(
                PAYMENT_ID,
                "1234567890",
                new BigDecimal("29.00"),
                "9999",
                PaymentStatus.PENDING_CONFIRMATION,
                "c".repeat(64)));
    when(fixture
            .receiptDao()
            .insertIfAbsent(any(), any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(1);

    PaymentCallbackProcessingResult result =
        fixture.service().processRedsysNotification(fixture.message());

    assertThat(result.status()).isEqualTo(PaymentStatus.CONFIRMED);
    assertThat(fixture.payment().getPaidAt()).isEqualTo(NOW.minusSeconds(60));
    verify(fixture.paymentDao(), never()).saveAndFlush(any());
    verifyNoInteractions(fixture.subscriptionService());
  }

  private Fixture fixture() {
    RedsysCallbackVerificationService verifier = mock(RedsysCallbackVerificationService.class);
    PaymentDao paymentDao = mock(PaymentDao.class);
    PaymentCallbackReceiptDao receiptDao = mock(PaymentCallbackReceiptDao.class);
    SubscriptionPaymentApplicationService subscriptionService =
        mock(SubscriptionPaymentApplicationService.class);
    AuditLogService auditLogService = mock(AuditLogService.class);
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
    payment.setStatus(PaymentStatus.PENDING_CONFIRMATION);
    payment.setCreatedAt(NOW.minusSeconds(120));
    payment.setUpdatedAt(NOW.minusSeconds(120));
    when(verifier.verify(message)).thenReturn(callback);
    when(paymentDao.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
    when(paymentDao.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
    var service =
        new PaymentCallbackProcessingServiceImpl(
            verifier,
            paymentDao,
            receiptDao,
            subscriptionService,
            auditLogService,
            Clock.fixed(NOW, ZoneOffset.UTC));
    return new Fixture(
        service,
        verifier,
        paymentDao,
        receiptDao,
        subscriptionService,
        auditLogService,
        payment,
        message);
  }

  private String responseCode(PaymentStatus status) {
    return switch (status) {
      case REJECTED -> "0101";
      case CANCELLED_BY_USER -> "9915";
      case COMMUNICATION_ERROR -> "0909";
      case PENDING_CONFIRMATION -> "9999";
      default -> throw new IllegalArgumentException("Unsupported fixture status");
    };
  }

  private record Fixture(
      PaymentCallbackProcessingServiceImpl service,
      RedsysCallbackVerificationService redsysVerifier,
      PaymentDao paymentDao,
      PaymentCallbackReceiptDao receiptDao,
      SubscriptionPaymentApplicationService subscriptionService,
      AuditLogService auditLogService,
      PaymentEntity payment,
      RedsysSignedMessage message) {}
}
