package com.reserly.platform.billing.payment;

import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.payment.redsys.InvalidPaymentCallbackException;
import com.reserly.platform.billing.payment.redsys.RedsysCallbackVerificationService;
import com.reserly.platform.billing.payment.redsys.RedsysPaymentProvider;
import com.reserly.platform.billing.payment.redsys.RedsysSignedMessage;
import com.reserly.platform.billing.payment.redsys.VerifiedRedsysCallback;
import com.reserly.platform.billing.persistence.PaymentCallbackReceiptDao;
import com.reserly.platform.billing.persistence.PaymentDao;
import com.reserly.platform.billing.persistence.PaymentEntity;
import com.reserly.platform.billing.service.PaymentConfirmation;
import com.reserly.platform.billing.service.SubscriptionPaymentApplicationService;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesa resultados sin persistir parametros firmados.
 *
 * <p>La firma se verifica antes de adquirir locks. Despues se bloquea el pago, se comprueban
 * importe/moneda/pedido, se inserta un recibo con ON CONFLICT y solo entonces puede aplicarse una
 * confirmacion.
 */
@Service
public class PaymentCallbackProcessingServiceImpl implements PaymentCallbackProcessingService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PaymentCallbackProcessingServiceImpl.class);

  private final RedsysCallbackVerificationService redsysVerifier;
  private final PaymentDao paymentDao;
  private final PaymentCallbackReceiptDao receiptDao;
  private final SubscriptionPaymentApplicationService subscriptionService;
  private final Clock clock;

  public PaymentCallbackProcessingServiceImpl(
      RedsysCallbackVerificationService redsysVerifier,
      PaymentDao paymentDao,
      PaymentCallbackReceiptDao receiptDao,
      SubscriptionPaymentApplicationService subscriptionService,
      Clock clock) {
    this.redsysVerifier = redsysVerifier;
    this.paymentDao = paymentDao;
    this.receiptDao = receiptDao;
    this.subscriptionService = subscriptionService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public PaymentCallbackProcessingResult inspectRedsysReturn(RedsysSignedMessage message) {
    VerifiedRedsysCallback callback = redsysVerifier.verify(message);
    PaymentEntity payment =
        paymentDao.findById(callback.paymentId()).orElseThrow(InvalidPaymentCallbackException::new);
    correlateRedsys(payment, callback);
    return new PaymentCallbackProcessingResult(callback.order(), callback.status(), false, false);
  }

  @Override
  @Transactional
  public PaymentCallbackProcessingResult processRedsysNotification(RedsysSignedMessage message) {
    VerifiedRedsysCallback callback = redsysVerifier.verify(message);
    PaymentEntity payment =
        paymentDao
            .findByIdForUpdate(callback.paymentId())
            .orElseThrow(InvalidPaymentCallbackException::new);
    correlateRedsys(payment, callback);
    return process(
        payment,
        callback.status(),
        callback.payloadHash(),
        "notification",
        callback.order(),
        callback.responseCode());
  }

  @Override
  @Transactional
  public PaymentCallbackProcessingResult processProviderResult(
      UUID paymentId, PaymentOrderResult result) {
    if (paymentId == null || result == null) {
      throw new IllegalArgumentException("Invalid provider result");
    }
    PaymentEntity payment =
        paymentDao.findByIdForUpdate(paymentId).orElseThrow(IllegalArgumentException::new);
    if (!result.provider().equals(payment.getProvider())
        || !result.providerOrderId().equals(payment.getProviderOrderId())
        || !result.requestPayloadHash().equals(payment.getRequestPayloadHash())) {
      throw new IllegalArgumentException("Provider result does not match payment");
    }
    return process(
        payment,
        result.status(),
        result.requestPayloadHash(),
        "simulator",
        result.providerOrderId(),
        null);
  }

  private PaymentCallbackProcessingResult process(
      PaymentEntity payment,
      PaymentStatus outcome,
      String payloadHash,
      String channel,
      String order,
      String providerResponseCode) {
    Instant receivedAt = clock.instant();
    int inserted =
        receiptDao.insertIfAbsent(
            UUID.randomUUID(),
            payment.getId(),
            payment.getProvider(),
            payment.getProviderOrderId(),
            channel,
            payloadHash,
            outcome.persistedValue(),
            receivedAt);
    if (inserted == 0) {
      LOGGER.info(
          "payment_callback_duplicate provider={} order={} outcome={}",
          payment.getProvider(),
          order,
          outcome.persistedValue());
      return new PaymentCallbackProcessingResult(order, outcome, true, false);
    }
    PaymentStatus previousOutcome = payment.getStatus();
    PaymentStatus persistedOutcome =
        persistOutcome(payment, outcome, channel, providerResponseCode, receivedAt);
    boolean subscriptionUpdated =
        persistedOutcome == PaymentStatus.CONFIRMED
            && previousOutcome != PaymentStatus.CONFIRMED
            && subscriptionService.applyConfirmedPayment(
                new PaymentConfirmation(
                    payment.getId(),
                    payment.getProvider(),
                    payment.getProviderOrderId(),
                    receivedAt));
    LOGGER.info(
        "payment_callback_accepted provider={} order={} outcome={} subscriptionUpdated={}",
        payment.getProvider(),
        order,
        persistedOutcome.persistedValue(),
        subscriptionUpdated);
    return new PaymentCallbackProcessingResult(order, persistedOutcome, false, subscriptionUpdated);
  }

  /**
   * Persiste una transición monotónica y un diagnóstico mínimo sin copiar el mensaje firmado.
   *
   * <p>La confirmación es absorbente. Rechazo y cancelación son terminales salvo una confirmación
   * posterior autenticada; error y pendiente pueden evolucionar. Esto evita que una notificación
   * atrasada degrade un resultado definitivo.
   */
  private PaymentStatus persistOutcome(
      PaymentEntity payment,
      PaymentStatus incoming,
      String channel,
      String providerResponseCode,
      Instant updatedAt) {
    PaymentStatus effective = effectiveOutcome(payment.getStatus(), incoming);
    if (payment.getStatus() == PaymentStatus.CONFIRMED
        || (effective == payment.getStatus() && effective != incoming)) {
      return effective;
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("channel", channel);
    response.put("outcome", effective.persistedValue());
    if (providerResponseCode != null) {
      response.put("providerResponseCode", providerResponseCode);
    }
    payment.setStatus(effective);
    payment.setResponsePayloadJson(response);
    payment.setPaidAt(effective == PaymentStatus.CONFIRMED ? updatedAt : null);
    payment.setUpdatedAt(updatedAt);
    paymentDao.saveAndFlush(payment);
    return effective;
  }

  private PaymentStatus effectiveOutcome(PaymentStatus current, PaymentStatus incoming) {
    if (current == PaymentStatus.CONFIRMED || incoming == PaymentStatus.CONFIRMED) {
      return PaymentStatus.CONFIRMED;
    }
    if (current == PaymentStatus.REJECTED || current == PaymentStatus.CANCELLED_BY_USER) {
      return current;
    }
    if (current == PaymentStatus.COMMUNICATION_ERROR
        && incoming == PaymentStatus.PENDING_CONFIRMATION) {
      return current;
    }
    return incoming;
  }

  private void correlateRedsys(PaymentEntity payment, VerifiedRedsysCallback callback) {
    if (!RedsysPaymentProvider.PROVIDER_CODE.equals(payment.getProvider())
        || !callback.order().equals(payment.getProviderOrderId())
        || callback.amount().compareTo(payment.getAmount()) != 0
        || !"EUR".equals(payment.getCurrency())) {
      throw new InvalidPaymentCallbackException();
    }
  }
}
