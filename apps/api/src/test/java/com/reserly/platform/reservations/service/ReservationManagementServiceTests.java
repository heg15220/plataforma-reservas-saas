package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationManagementServiceTests {

  private final ReservationDao dao = mock(ReservationDao.class);
  private final OneTimeTokenService tokens = mock(OneTimeTokenService.class);
  private final ReservationCancellationPolicy policy = mock(ReservationCancellationPolicy.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);
  private final ReservationManagementService service =
      new ReservationManagementServiceImpl(dao, tokens, policy, clock);

  @BeforeEach
  void configureCancellationWindow() {
    when(policy.evaluate(any(), any(), anyInt(), any(), any()))
        .thenReturn(
            new ReservationCancellationPolicy.CancellationWindow(
                Instant.parse("2026-07-31T10:00:00Z"), true));
  }

  @Test
  void returnsOnlyReservationBoundToValidUnexpiredToken() {
    String token = "a".repeat(43);
    when(tokens.isValid(token)).thenReturn(true);
    when(tokens.hash(token)).thenReturn("hash");
    ReservationEntity reservation = reservation(Instant.parse("2026-07-23T12:00:00Z"));
    when(dao.findBySecureTokenHash("hash")).thenReturn(Optional.of(reservation));

    var response = service.findByToken(token);

    assertThat(response.reservationId()).isEqualTo(reservation.getId());
    assertThat(response.venueName()).isEqualTo("Local Centro");
    assertThat(response.status()).isEqualTo("confirmed");
    assertThat(response.cancellable()).isTrue();
    verify(dao).findBySecureTokenHash("hash");
  }

  @Test
  void cancelsConfirmedReservationAndRevokesManagementToken() {
    String token = "c".repeat(43);
    when(tokens.isValid(token)).thenReturn(true);
    when(tokens.hash(token)).thenReturn("hash");
    ReservationEntity reservation = reservation(Instant.parse("2026-07-23T12:00:00Z"));
    reservation.setSecureTokenHash("hash");
    when(dao.findBySecureTokenHashForUpdate("hash")).thenReturn(Optional.of(reservation));

    var response = service.cancelByToken(token);

    assertThat(response.status()).isEqualTo("cancelled_by_user");
    assertThat(reservation.getStatus()).isEqualTo("cancelled_by_user");
    assertThat(reservation.getCancelledBy()).isEqualTo("customer");
    assertThat(reservation.getCancellationReason()).isEqualTo("customer_request");
    assertThat(reservation.getSecureTokenHash()).isNull();
    assertThat(reservation.getSecureTokenExpiresAt()).isNull();
    verify(dao).save(reservation);
  }

  @Test
  void rejectsCancellationOutsideConfiguredWindowWithoutMutation() {
    String token = "d".repeat(43);
    when(tokens.isValid(token)).thenReturn(true);
    when(tokens.hash(token)).thenReturn("hash");
    ReservationEntity reservation = reservation(Instant.parse("2026-07-23T12:00:00Z"));
    when(dao.findBySecureTokenHashForUpdate("hash")).thenReturn(Optional.of(reservation));
    when(policy.evaluate(any(), any(), anyInt(), any(), any()))
        .thenReturn(
            new ReservationCancellationPolicy.CancellationWindow(
                Instant.parse("2026-07-21T10:00:00Z"), false));

    assertThatThrownBy(() -> service.cancelByToken(token))
        .isInstanceOf(ReservationCancellationNotAllowedException.class);

    assertThat(reservation.getStatus()).isEqualTo("confirmed");
    verify(dao, never()).save(any());
  }

  @Test
  void rejectsMalformedTokenWithoutDatabaseLookup() {
    when(tokens.isValid("bad")).thenReturn(false);

    assertThatThrownBy(() -> service.findByToken("bad"))
        .isInstanceOf(ReservationManagementNotFoundException.class);
    verifyNoInteractions(dao);
    verify(tokens, never()).hash(any());
  }

  @Test
  void returnsSameSecureErrorForExpiredToken() {
    String token = "b".repeat(43);
    when(tokens.isValid(token)).thenReturn(true);
    when(tokens.hash(token)).thenReturn("hash");
    when(dao.findBySecureTokenHash("hash"))
        .thenReturn(Optional.of(reservation(Instant.parse("2026-07-22T12:00:00Z"))));

    assertThatThrownBy(() -> service.findByToken(token))
        .isInstanceOf(ReservationManagementNotFoundException.class);
  }

  @Test
  void rejectsMalformedCancellationTokenBeforeDatabaseLookup() {
    when(tokens.isValid("invalid")).thenReturn(false);

    assertThatThrownBy(() -> service.cancelByToken("invalid"))
        .isInstanceOf(ReservationManagementNotFoundException.class);

    verifyNoInteractions(dao);
    verify(tokens, never()).hash(any());
  }

  @Test
  void rejectsExpiredCancellationTokenWithoutChangingReservation() {
    String token = "e".repeat(43);
    when(tokens.isValid(token)).thenReturn(true);
    when(tokens.hash(token)).thenReturn("hash");
    ReservationEntity reservation = reservation(clock.instant());
    when(dao.findBySecureTokenHashForUpdate("hash")).thenReturn(Optional.of(reservation));

    assertThatThrownBy(() -> service.cancelByToken(token))
        .isInstanceOf(ReservationManagementNotFoundException.class);

    assertThat(reservation.getStatus()).isEqualTo("confirmed");
    verify(dao, never()).save(any());
  }

  private ReservationEntity reservation(Instant expiresAt) {
    VenueEntity venue = new VenueEntity();
    venue.setName("Local Centro");
    venue.setAddress("Calle Mayor 1");
    ReservationEntity value = new ReservationEntity();
    value.setId(UUID.randomUUID());
    value.setVenue(venue);
    value.setDate(LocalDate.of(2026, 8, 1));
    value.setStartsAt(LocalTime.of(10, 0));
    value.setEndsAt(LocalTime.of(11, 0));
    value.setPartySize(2);
    value.setStatus("confirmed");
    value.setSecureTokenExpiresAt(expiresAt);
    return value;
  }
}
