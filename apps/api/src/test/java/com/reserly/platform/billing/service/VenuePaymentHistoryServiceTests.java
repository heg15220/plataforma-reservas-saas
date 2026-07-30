package com.reserly.platform.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.billing.PaymentStatus;
import com.reserly.platform.billing.persistence.PaymentDao;
import com.reserly.platform.billing.persistence.PaymentEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/** Verifica ownership, límite y minimización del historial de facturación. */
class VenuePaymentHistoryServiceTests {

  private static final UUID OWNER_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
  private static final UUID VENUE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

  @Test
  void returnsOnlyRecentOwnedPaymentsAsMinimizedProjection() {
    VenueDao venueDao = mock(VenueDao.class);
    PaymentDao paymentDao = mock(PaymentDao.class);
    VenueEntity venue = mock(VenueEntity.class);
    PaymentEntity payment = payment();
    when(venue.getId()).thenReturn(VENUE_ID);
    when(venueDao.findCurrentByOwnerUserId(OWNER_ID)).thenReturn(Optional.of(venue));
    when(paymentDao.findHistoryByVenueId(VENUE_ID, PageRequest.of(0, 50)))
        .thenReturn(List.of(payment));

    var response = new VenuePaymentHistoryServiceImpl(venueDao, paymentDao).findOwned(OWNER_ID);

    assertThat(response.payments()).hasSize(1);
    assertThat(response.payments().getFirst())
        .extracting(
            "orderReference", "amount", "currency", "status", "createdAt", "paidAt")
        .containsExactly(
            "ORDER12345",
            new BigDecimal("29.00"),
            "EUR",
            "confirmed",
            Instant.parse("2026-07-30T10:00:00Z"),
            Instant.parse("2026-07-30T10:01:00Z"));
    verify(paymentDao).findHistoryByVenueId(eq(VENUE_ID), eq(PageRequest.of(0, 50)));
  }

  @Test
  void rejectsUnknownOwnerBeforeReadingPayments() {
    VenueDao venueDao = mock(VenueDao.class);
    PaymentDao paymentDao = mock(PaymentDao.class);
    when(venueDao.findCurrentByOwnerUserId(OWNER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> new VenuePaymentHistoryServiceImpl(venueDao, paymentDao).findOwned(OWNER_ID))
        .isInstanceOf(VenueSubscriptionNotFoundException.class);
    verifyNoInteractions(paymentDao);
  }

  private PaymentEntity payment() {
    PaymentEntity payment = new PaymentEntity();
    payment.setProviderOrderId("ORDER12345");
    payment.setAmount(new BigDecimal("29.00"));
    payment.setCurrency("EUR");
    payment.setStatus(PaymentStatus.CONFIRMED);
    payment.setCreatedAt(Instant.parse("2026-07-30T10:00:00Z"));
    payment.setPaidAt(Instant.parse("2026-07-30T10:01:00Z"));
    return payment;
  }
}
