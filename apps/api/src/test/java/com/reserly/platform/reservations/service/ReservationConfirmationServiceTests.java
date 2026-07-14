package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ReservationConfirmRequest;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.persistence.ReservationTimeSlotDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Cubre propiedad, vigencia y capacidad real antes de la transición confirmada. */
@ExtendWith(MockitoExtension.class)
class ReservationConfirmationServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-14T10:00:00Z");
  private static final String TOKEN = "A".repeat(43);
  private static final String TOKEN_HASH = "a".repeat(64);

  @Mock private ReservationDao reservationDao;
  @Mock private ReservationTimeSlotDao timeSlotDao;
  @Mock private OneTimeTokenService tokenService;

  private ReservationConfirmationServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ReservationConfirmationServiceImpl(
            reservationDao,
            timeSlotDao,
            tokenService,
            new ReservationHoldExpirationPolicyImpl(),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void confirmsOwnedHoldAndConsumesItsOneTimeSecret() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    when(reservationDao.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);
    when(timeSlotDao.findByIdForUpdate(reservation.getTimeSlot().getId()))
        .thenReturn(Optional.of(reservation.getTimeSlot()));
    when(reservationDao.sumOccupiedCapacityExcluding(
            reservation.getTimeSlot().getId(), reservationId, NOW))
        .thenReturn(2L);
    when(reservationDao.save(reservation)).thenReturn(reservation);

    var response = service.confirm(reservationId, request(TOKEN));

    assertThat(response.status()).isEqualTo("confirmed");
    assertThat(response.reservationId()).isEqualTo(reservationId);
    assertThat(response.manageUrlSentTo()).isEqualTo("Maria@Example.COM");
    assertThat(response.venueName()).isEqualTo("Local de prueba");
    assertThat(reservation.getCustomerName()).isEqualTo("María López");
    assertThat(reservation.getCustomerEmailNormalized()).isEqualTo("maria@example.com");
    assertThat(reservation.getHoldTokenHash()).isNull();
    assertThat(reservation.getHoldExpiresAt()).isNull();
    assertThat(reservation.getUpdatedAt()).isEqualTo(NOW);
    verify(reservationDao).save(reservation);
  }

  @Test
  void rejectsTokenThatDoesNotOwnTheHold() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    when(reservationDao.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn("b".repeat(64));

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN)))
        .isInstanceOf(ReservationConfirmationInvalidException.class);

    verify(timeSlotDao, never()).findByIdForUpdate(any());
    verify(reservationDao, never()).save(any());
  }

  @Test
  void rejectsHoldAtItsExclusiveExpirationBoundary() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    reservation.setHoldExpiresAt(NOW);
    when(reservationDao.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN)))
        .isInstanceOf(ReservationHoldExpiredException.class);

    verify(timeSlotDao, never()).findByIdForUpdate(any());
    verify(reservationDao, never()).save(any());
  }

  @Test
  void rejectsWhenOtherOccupantsNoLongerLeaveEnoughCapacity() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    when(reservationDao.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);
    when(timeSlotDao.findByIdForUpdate(reservation.getTimeSlot().getId()))
        .thenReturn(Optional.of(reservation.getTimeSlot()));
    when(reservationDao.sumOccupiedCapacityExcluding(
            reservation.getTimeSlot().getId(), reservationId, NOW))
        .thenReturn(3L);

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN)))
        .isInstanceOf(ReservationCapacityUnavailableException.class);

    verify(reservationDao, never()).save(any());
  }

  @Test
  void invalidTokenDoesNotRevealThatHoldIsExpired() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    reservation.setHoldExpiresAt(NOW.minusSeconds(1));
    when(reservationDao.findByIdForUpdate(reservationId))
        .thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn("b".repeat(64));

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN)))
        .isInstanceOf(ReservationConfirmationInvalidException.class);
  }

  @Test
  void rejectsUnimplementedCustomResponsesInsteadOfIgnoringThem() {
    ReservationConfirmRequest request =
        new ReservationConfirmRequest(
            TOKEN,
            "María López",
            "maria@example.com",
            2,
            List.of(
                new com.reserly.platform.reservations.dto.ReservationConfirmFormResponse(
                    UUID.randomUUID(),
                    com.fasterxml.jackson.databind.node.TextNode.valueOf("Sin gluten"))),
            true,
            true);

    assertThatThrownBy(() -> service.confirm(UUID.randomUUID(), request))
        .isInstanceOf(ReservationConfirmationInvalidException.class);

    verify(reservationDao, never()).findByIdForUpdate(any());
  }

  private ReservationConfirmRequest request(String token) {
    return new ReservationConfirmRequest(
        token,
        "  María López  ",
        "  Maria@Example.COM  ",
        2,
        List.of(),
        true,
        true);
  }

  private ReservationEntity reservation(UUID reservationId) {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setName("Local de prueba");
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(UUID.randomUUID());
    slot.setVenue(venue);
    slot.setCapacity(4);
    slot.setDate(LocalDate.of(2026, 7, 15));
    slot.setStartsAt(LocalTime.of(11, 0));
    slot.setEndsAt(LocalTime.of(12, 0));

    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(reservationId);
    reservation.setVenue(venue);
    reservation.setTimeSlot(slot);
    reservation.setPartySize(2);
    reservation.setDate(slot.getDate());
    reservation.setStartsAt(slot.getStartsAt());
    reservation.setEndsAt(slot.getEndsAt());
    reservation.setStatus("hold");
    reservation.setHoldExpiresAt(NOW.plusSeconds(300));
    reservation.setHoldTokenHash(TOKEN_HASH);
    reservation.setCreatedAt(NOW.minusSeconds(30));
    reservation.setUpdatedAt(NOW.minusSeconds(30));
    return reservation;
  }
}
