package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
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
import org.junit.jupiter.api.Test;

class ReservationManagementServiceTests {

  private final ReservationDao dao = mock(ReservationDao.class);
  private final OneTimeTokenService tokens = mock(OneTimeTokenService.class);
  private final Clock clock = Clock.fixed(Instant.parse("2026-07-22T12:00:00Z"), ZoneOffset.UTC);
  private final ReservationManagementService service =
      new ReservationManagementServiceImpl(dao, tokens, clock);

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
    verify(dao).findBySecureTokenHash("hash");
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
