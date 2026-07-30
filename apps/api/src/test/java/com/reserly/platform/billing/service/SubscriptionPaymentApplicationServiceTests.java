package com.reserly.platform.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.billing.BillingPeriod;
import com.reserly.platform.billing.SubscriptionStatus;
import com.reserly.platform.billing.persistence.PaymentDao;
import com.reserly.platform.billing.persistence.PaymentEntity;
import com.reserly.platform.billing.persistence.SubscriptionDao;
import com.reserly.platform.billing.persistence.SubscriptionEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica activacion, renovacion, correlacion e idempotencia por pago aplicado. */
class SubscriptionPaymentApplicationServiceTests {

  private static final UUID PAYMENT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
  private static final UUID SUBSCRIPTION_ID =
      UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final UUID VENUE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
  private static final UUID PLAN_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
  private static final Instant CONFIRMED_AT = Instant.parse("2026-07-30T10:00:00Z");

  @Test
  void activatesPendingMonthlySubscriptionAndMarksPaymentAsApplied() {
    Fixture fixture = fixture(BillingPeriod.MONTHLY);

    boolean updated = fixture.service().applyConfirmedPayment(confirmation());

    assertThat(updated).isTrue();
    assertThat(fixture.subscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(fixture.subscription().getCurrentPeriodStartsAt()).isEqualTo(CONFIRMED_AT);
    assertThat(fixture.subscription().getCurrentPeriodEndsAt())
        .isEqualTo(Instant.parse("2026-08-30T10:00:00Z"));
    assertThat(fixture.subscription().getLastAppliedPaymentId()).isEqualTo(PAYMENT_ID);
    verify(fixture.subscriptionDao()).saveAndFlush(fixture.subscription());
  }

  @Test
  void extendsActiveYearlyPeriodFromCurrentEndInsteadOfOverwritingRemainingTime() {
    Fixture fixture = fixture(BillingPeriod.YEARLY);
    fixture.subscription().setStatus(SubscriptionStatus.ACTIVE);
    fixture.subscription().setCurrentPeriodStartsAt(Instant.parse("2025-08-15T10:00:00Z"));
    fixture.subscription().setCurrentPeriodEndsAt(Instant.parse("2026-08-15T10:00:00Z"));

    fixture.service().applyConfirmedPayment(confirmation());

    assertThat(fixture.subscription().getCurrentPeriodStartsAt())
        .isEqualTo(Instant.parse("2026-08-15T10:00:00Z"));
    assertThat(fixture.subscription().getCurrentPeriodEndsAt())
        .isEqualTo(Instant.parse("2027-08-15T10:00:00Z"));
  }

  @Test
  void ignoresSamePaymentWithoutExtendingPeriodAgain() {
    Fixture fixture = fixture(BillingPeriod.MONTHLY);
    fixture.subscription().setLastAppliedPaymentId(PAYMENT_ID);
    Instant existingEnd = Instant.parse("2026-08-30T10:00:00Z");
    fixture.subscription().setCurrentPeriodEndsAt(existingEnd);

    boolean updated = fixture.service().applyConfirmedPayment(confirmation());

    assertThat(updated).isFalse();
    assertThat(fixture.subscription().getCurrentPeriodEndsAt()).isEqualTo(existingEnd);
    verify(fixture.subscriptionDao(), never()).saveAndFlush(fixture.subscription());
  }

  @Test
  void rejectsProviderMismatchAndCancelledSubscription() {
    Fixture wrongProvider = fixture(BillingPeriod.MONTHLY);
    PaymentConfirmation mismatched =
        new PaymentConfirmation(PAYMENT_ID, "redsys", "sim_confirmed_001", CONFIRMED_AT);
    assertThatThrownBy(() -> wrongProvider.service().applyConfirmedPayment(mismatched))
        .isInstanceOf(SubscriptionPaymentApplicationException.class);

    Fixture cancelled = fixture(BillingPeriod.MONTHLY);
    cancelled.subscription().setStatus(SubscriptionStatus.CANCELLED);
    assertThatThrownBy(() -> cancelled.service().applyConfirmedPayment(confirmation()))
        .isInstanceOf(SubscriptionPaymentApplicationException.class);
  }

  private Fixture fixture(BillingPeriod period) {
    PaymentDao paymentDao = mock(PaymentDao.class);
    SubscriptionDao subscriptionDao = mock(SubscriptionDao.class);
    PaymentEntity payment = new PaymentEntity();
    payment.setId(PAYMENT_ID);
    payment.setSubscriptionId(SUBSCRIPTION_ID);
    payment.setVenueId(VENUE_ID);
    payment.setProvider("simulated");
    payment.setProviderOrderId("sim_confirmed_001");
    payment.setAmount(new BigDecimal("29.00"));
    payment.setCurrency("EUR");
    SubscriptionEntity subscription = new SubscriptionEntity();
    subscription.setId(SUBSCRIPTION_ID);
    subscription.setVenueId(VENUE_ID);
    subscription.setPlanId(PLAN_ID);
    subscription.setStatus(SubscriptionStatus.PENDING_PAYMENT);
    subscription.setBillingPeriod(period);
    when(paymentDao.findByIdForUpdate(PAYMENT_ID)).thenReturn(Optional.of(payment));
    when(subscriptionDao.findByIdForUpdate(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
    return new Fixture(
        new SubscriptionPaymentApplicationServiceImpl(paymentDao, subscriptionDao),
        subscriptionDao,
        subscription);
  }

  private PaymentConfirmation confirmation() {
    return new PaymentConfirmation(PAYMENT_ID, "simulated", "sim_confirmed_001", CONFIRMED_AT);
  }

  private record Fixture(
      SubscriptionPaymentApplicationServiceImpl service,
      SubscriptionDao subscriptionDao,
      SubscriptionEntity subscription) {}
}
