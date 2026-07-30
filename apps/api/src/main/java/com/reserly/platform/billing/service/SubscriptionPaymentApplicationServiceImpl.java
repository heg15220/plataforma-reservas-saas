package com.reserly.platform.billing.service;

import com.reserly.platform.billing.BillingPeriod;
import com.reserly.platform.billing.SubscriptionStatus;
import com.reserly.platform.billing.persistence.PaymentDao;
import com.reserly.platform.billing.persistence.PaymentEntity;
import com.reserly.platform.billing.persistence.SubscriptionDao;
import com.reserly.platform.billing.persistence.SubscriptionEntity;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serializa la aplicacion de pagos confirmados y calcula el siguiente periodo en UTC.
 *
 * <p>El pago debe coincidir con la suscripcion y con la orden persistida que fijo el importe. Una
 * suscripcion cancelada nunca se reactiva implicitamente.
 */
@Service
public class SubscriptionPaymentApplicationServiceImpl
    implements SubscriptionPaymentApplicationService {

  private final PaymentDao paymentDao;
  private final SubscriptionDao subscriptionDao;

  public SubscriptionPaymentApplicationServiceImpl(
      PaymentDao paymentDao, SubscriptionDao subscriptionDao) {
    this.paymentDao = paymentDao;
    this.subscriptionDao = subscriptionDao;
  }

  @Override
  @Transactional
  public boolean applyConfirmedPayment(PaymentConfirmation confirmation) {
    PaymentEntity payment =
        paymentDao
            .findByIdForUpdate(confirmation.paymentId())
            .orElseThrow(SubscriptionPaymentApplicationException::new);
    requirePaymentCorrelation(payment, confirmation);
    SubscriptionEntity subscription =
        subscriptionDao
            .findByIdForUpdate(payment.getSubscriptionId())
            .orElseThrow(SubscriptionPaymentApplicationException::new);
    requireSubscriptionCorrelation(subscription, payment);

    if (payment.getId().equals(subscription.getLastAppliedPaymentId())) {
      return false;
    }
    if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
      throw new SubscriptionPaymentApplicationException();
    }
    Instant periodStart = nextPeriodStart(subscription, confirmation.confirmedAt());
    Instant periodEnd =
        subscription.getBillingPeriod() == BillingPeriod.YEARLY
            ? periodStart.atZone(ZoneOffset.UTC).plusYears(1).toInstant()
            : periodStart.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setCurrentPeriodStartsAt(periodStart);
    subscription.setCurrentPeriodEndsAt(periodEnd);
    subscription.setTrialEndsAt(null);
    subscription.setCancelledAt(null);
    subscription.setLastAppliedPaymentId(payment.getId());
    subscription.setUpdatedAt(confirmation.confirmedAt());
    subscriptionDao.saveAndFlush(subscription);
    return true;
  }

  private void requirePaymentCorrelation(PaymentEntity payment, PaymentConfirmation confirmation) {
    if (!confirmation.provider().equals(payment.getProvider())
        || !confirmation.providerOrderId().equals(payment.getProviderOrderId())
        || !"EUR".equals(payment.getCurrency())) {
      throw new SubscriptionPaymentApplicationException();
    }
  }

  private void requireSubscriptionCorrelation(
      SubscriptionEntity subscription, PaymentEntity payment) {
    if (!subscription.getVenueId().equals(payment.getVenueId())
        || subscription.getBillingPeriod() == null) {
      throw new SubscriptionPaymentApplicationException();
    }
  }

  private Instant nextPeriodStart(SubscriptionEntity subscription, Instant confirmedAt) {
    if (subscription.getStatus() == SubscriptionStatus.ACTIVE
        && subscription.getCurrentPeriodEndsAt() != null
        && subscription.getCurrentPeriodEndsAt().isAfter(confirmedAt)) {
      return subscription.getCurrentPeriodEndsAt();
    }
    return confirmedAt;
  }
}
