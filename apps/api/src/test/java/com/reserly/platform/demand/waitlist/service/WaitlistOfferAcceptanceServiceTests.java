package com.reserly.platform.demand.waitlist.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.waitlist.dto.WaitlistOfferAcceptanceRequest;
import com.reserly.platform.demand.waitlist.persistence.WaitlistEntryDao;
import com.reserly.platform.demand.waitlist.persistence.WaitlistEntryEntity;
import com.reserly.platform.demand.waitlist.persistence.WaitlistOfferDao;
import com.reserly.platform.demand.waitlist.persistence.WaitlistOfferEntity;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import com.reserly.platform.reservations.service.ReservationHoldInvalidException;
import com.reserly.platform.reservations.service.ReservationHoldService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica consumo único y delegación al hold autoritativo sin confiar en campos del cliente. */
@ExtendWith(MockitoExtension.class)
class WaitlistOfferAcceptanceServiceTests {

  private static final Instant NOW = Instant.parse("2026-08-20T15:00:00Z");
  private static final String TOKEN = "A".repeat(43);
  private static final String TOKEN_HASH = "a".repeat(64);

  @Mock private WaitlistOfferDao offerDao;
  @Mock private WaitlistEntryDao entryDao;
  @Mock private ReservationHoldService holdService;
  @Mock private OneTimeTokenService tokenService;

  private WaitlistOfferAcceptanceServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new WaitlistOfferAcceptanceServiceImpl(
            offerDao, entryDao, holdService, tokenService, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void createsOrdinaryHoldAndConsumesOfferInSameUseCase() {
    WaitlistOfferEntity offer = offer("active", NOW.minusSeconds(60), NOW.plusSeconds(60));
    WaitlistEntryEntity entry = entry("offered");
    UUID reservationId = UUID.randomUUID();
    ReservationHoldResponse expected =
        new ReservationHoldResponse(reservationId, "B".repeat(43), NOW.plusSeconds(300), 300);
    stubValidTokenAndRows(offer, entry);
    when(holdService.create(any(ReservationHoldRequest.class))).thenReturn(expected);

    ReservationHoldResponse result =
        service.accept(TOKEN, new WaitlistOfferAcceptanceRequest(null, "any_available"));

    assertThat(result).isSameAs(expected);
    ArgumentCaptor<ReservationHoldRequest> request =
        ArgumentCaptor.forClass(ReservationHoldRequest.class);
    verify(holdService).create(request.capture());
    assertThat(request.getValue().venueId()).isEqualTo(entry.getVenueId());
    assertThat(request.getValue().timeSlotId()).isEqualTo(entry.getTimeSlotId());
    assertThat(request.getValue().serviceId()).isEqualTo(entry.getServiceId());
    assertThat(request.getValue().partySize()).isEqualTo(entry.getPartySize());
    assertThat(offer.getStatus()).isEqualTo("accepted");
    assertThat(offer.getAcceptedReservationId()).isEqualTo(reservationId);
    assertThat(entry.getStatus()).isEqualTo("accepted");
    verify(offerDao).save(offer);
    verify(entryDao).save(entry);
  }

  @Test
  void rejectsNotYetActiveExpiredAndAlreadyConsumedOffers() {
    for (WaitlistOfferEntity offer :
        new WaitlistOfferEntity[] {
          offer("scheduled", NOW.plusSeconds(1), NOW.plusSeconds(60)),
          offer("active", NOW.minusSeconds(60), NOW),
          offer("accepted", NOW.minusSeconds(60), NOW.plusSeconds(60))
        }) {
      when(tokenService.isValid(TOKEN)).thenReturn(true);
      when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);
      when(offerDao.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(offer));
      assertThatThrownBy(
              () -> service.accept(TOKEN, new WaitlistOfferAcceptanceRequest(null, null)))
          .isInstanceOf(WaitlistOfferUnavailableException.class);
    }
    verify(holdService, never()).create(any());
  }

  @Test
  void rejectsRevokedConsentBeforeCreatingHold() {
    WaitlistOfferEntity offer = offer("active", NOW.minusSeconds(60), NOW.plusSeconds(60));
    WaitlistEntryEntity entry = entry("offered");
    entry.setContactRevokedAt(NOW.minusSeconds(1));
    stubValidTokenAndRows(offer, entry);

    assertThatThrownBy(() -> service.accept(TOKEN, new WaitlistOfferAcceptanceRequest(null, null)))
        .isInstanceOf(WaitlistOfferUnavailableException.class);
    verify(holdService, never()).create(any());
  }

  @Test
  void capacityFailureDoesNotConsumeOffer() {
    WaitlistOfferEntity offer = offer("active", NOW.minusSeconds(60), NOW.plusSeconds(60));
    WaitlistEntryEntity entry = entry("offered");
    stubValidTokenAndRows(offer, entry);
    when(holdService.create(any())).thenThrow(new ReservationHoldInvalidException());

    assertThatThrownBy(() -> service.accept(TOKEN, new WaitlistOfferAcceptanceRequest(null, null)))
        .isInstanceOf(WaitlistOfferUnavailableException.class);
    assertThat(offer.getStatus()).isEqualTo("active");
    assertThat(entry.getStatus()).isEqualTo("offered");
    verify(offerDao, never()).save(any());
    verify(entryDao, never()).save(any());
  }

  @Test
  void rejectsMalformedTokenBeforeHashOrDatabaseLookup() {
    when(tokenService.isValid("invalid")).thenReturn(false);
    assertThatThrownBy(
            () -> service.accept("invalid", new WaitlistOfferAcceptanceRequest(null, null)))
        .isInstanceOf(WaitlistOfferUnavailableException.class);
    verify(tokenService, never()).hash(any());
    verify(offerDao, never()).findByTokenHashForUpdate(any());
  }

  private void stubValidTokenAndRows(WaitlistOfferEntity offer, WaitlistEntryEntity entry) {
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);
    when(offerDao.findByTokenHashForUpdate(TOKEN_HASH)).thenReturn(Optional.of(offer));
    when(entryDao.findByIdForUpdate(offer.getWaitlistEntryId())).thenReturn(Optional.of(entry));
  }

  private WaitlistOfferEntity offer(String status, Instant availableAt, Instant expiresAt) {
    WaitlistOfferEntity offer = new WaitlistOfferEntity();
    offer.setId(UUID.randomUUID());
    offer.setWaitlistEntryId(UUID.randomUUID());
    offer.setStatus(status);
    offer.setAvailableAt(availableAt);
    offer.setExpiresAt(expiresAt);
    offer.setUpdatedAt(NOW.minusSeconds(60));
    return offer;
  }

  private WaitlistEntryEntity entry(String status) {
    WaitlistEntryEntity entry = new WaitlistEntryEntity();
    entry.setId(UUID.randomUUID());
    entry.setVenueId(UUID.randomUUID());
    entry.setTimeSlotId(UUID.randomUUID());
    entry.setServiceId(UUID.randomUUID());
    entry.setPartySize(2);
    entry.setStatus(status);
    entry.setUpdatedAt(NOW.minusSeconds(60));
    return entry;
  }
}
