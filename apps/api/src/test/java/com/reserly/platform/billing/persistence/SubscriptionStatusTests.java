package com.reserly.platform.billing.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.billing.BillingPeriod;
import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.SubscriptionStatus;
import org.junit.jupiter.api.Test;

/** Alinea los catálogos Java con los valores exactos protegidos por PostgreSQL. */
class SubscriptionStatusTests {

  private final SubscriptionStatusConverter subscriptionConverter =
      new SubscriptionStatusConverter();
  private final BillingPeriodConverter billingPeriodConverter = new BillingPeriodConverter();
  private final PaymentStatusConverter paymentStatusConverter = new PaymentStatusConverter();

  @Test
  void roundTripsEverySubscriptionState() {
    for (SubscriptionStatus status : SubscriptionStatus.values()) {
      String persisted = subscriptionConverter.convertToDatabaseColumn(status);
      assertThat(subscriptionConverter.convertToEntityAttribute(persisted)).isEqualTo(status);
    }

    assertThat(SubscriptionStatus.values())
        .extracting(SubscriptionStatus::persistedValue)
        .containsExactly("trial", "active", "pending_payment", "suspended", "cancelled");
  }

  @Test
  void roundTripsDependentBillingAndPaymentCatalogs() {
    for (BillingPeriod period : BillingPeriod.values()) {
      String persisted = billingPeriodConverter.convertToDatabaseColumn(period);
      assertThat(billingPeriodConverter.convertToEntityAttribute(persisted)).isEqualTo(period);
    }

    for (PaymentStatus status : PaymentStatus.values()) {
      String persisted = paymentStatusConverter.convertToDatabaseColumn(status);
      assertThat(paymentStatusConverter.convertToEntityAttribute(persisted)).isEqualTo(status);
    }
  }

  @Test
  void rejectsUnknownPersistedValuesAndPreservesNulls() {
    assertThat(subscriptionConverter.convertToDatabaseColumn(null)).isNull();
    assertThat(subscriptionConverter.convertToEntityAttribute(null)).isNull();
    assertThatThrownBy(() -> subscriptionConverter.convertToEntityAttribute("ACTIVE"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported subscription status");
    assertThatThrownBy(() -> paymentStatusConverter.convertToEntityAttribute("paid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Unsupported payment status");
  }
}
