package com.reserly.platform.reservations.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.TextNode;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.forms.dto.ValidatedReservationFormAnswer;
import com.reserly.platform.forms.service.ReservationFormConfirmationService;
import com.reserly.platform.forms.service.ReservationFormResponseInvalidException;
import com.reserly.platform.forms.service.ReservationFormResponseViolation;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.reservations.dto.ReservationConfirmFormResponse;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/** Cubre la confirmación atómica, el formulario y la emisión segura del trabajo de correo. */
@ExtendWith(MockitoExtension.class)
class ReservationConfirmationServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-14T10:00:00Z");
  private static final Instant MANAGE_EXPIRY = Instant.parse("2026-08-14T12:00:00Z");
  private static final String TOKEN = "A".repeat(43);
  private static final String TOKEN_HASH = "a".repeat(64);
  private static final String MANAGE_TOKEN = "B".repeat(43);
  private static final String MANAGE_HASH = "b".repeat(64);

  @Mock private ReservationDao reservationDao;
  @Mock private ReservationTimeSlotDao timeSlotDao;
  @Mock private OneTimeTokenService tokenService;
  @Mock private ReservationFormConfirmationService formConfirmationService;
  @Mock private ReservationManagementTokenPolicy managementTokenPolicy;
  @Mock private ApplicationEventPublisher eventPublisher;
  private ReservationConfirmationServiceImpl service;

  @BeforeEach
  void setUp() {
    service =
        new ReservationConfirmationServiceImpl(
            reservationDao,
            timeSlotDao,
            tokenService,
            new ReservationHoldExpirationPolicyImpl(),
            formConfirmationService,
            managementTokenPolicy,
            eventPublisher,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void confirmsOwnedHoldPersistsManagementHashAndPublishesEmailWork() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    arrangeValidHold(reservation);
    UUID fieldId = UUID.randomUUID();
    var validated =
        new ValidatedReservationFormAnswer(
            fieldId, "allergies", "Alergias", "short_text", TextNode.valueOf("Sin gluten"));
    when(formConfirmationService.validateAndPersist(any(), any(), any(), any()))
        .thenReturn(List.of(validated));
    when(tokenService.generate()).thenReturn(MANAGE_TOKEN);
    when(tokenService.hash(MANAGE_TOKEN)).thenReturn(MANAGE_HASH);
    when(managementTokenPolicy.expiresAt(any(), any(), any())).thenReturn(MANAGE_EXPIRY);
    when(reservationDao.save(reservation)).thenReturn(reservation);

    var response =
        service.confirm(
            reservationId,
            request(
                TOKEN,
                List.of(
                    new ReservationConfirmFormResponse(fieldId, TextNode.valueOf("Sin gluten")))));

    assertThat(response.status()).isEqualTo("confirmed");
    assertThat(reservation.getSecureTokenHash()).isEqualTo(MANAGE_HASH);
    assertThat(reservation.getSecureTokenExpiresAt()).isEqualTo(MANAGE_EXPIRY);
    assertThat(reservation.getHoldTokenHash()).isNull();
    ArgumentCaptor<ReservationConfirmationEmailRequestedEvent> event =
        ArgumentCaptor.forClass(ReservationConfirmationEmailRequestedEvent.class);
    verify(eventPublisher).publishEvent(event.capture());
    assertThat(event.getValue().manageToken()).isEqualTo(MANAGE_TOKEN);
    assertThat(event.getValue().customerEmail()).isEqualTo("Maria@Example.COM");
    assertThat(event.getValue().venueEmail()).isEqualTo("owner@example.com");
    assertThat(event.getValue().customerLocale()).isEqualTo("en");
    assertThat(event.getValue().venueLocale()).isEqualTo("es");
    assertThat(event.getValue().formResponses())
        .singleElement()
        .extracting(ReservationConfirmationEmailAnswer::valueJson)
        .isEqualTo("\"Sin gluten\"");
  }

  @Test
  void invalidFormRollsBackBeforeGeneratingCredentialOrPublishingWork() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    arrangeValidHold(reservation);
    when(formConfirmationService.validateAndPersist(any(), any(), any(), any()))
        .thenThrow(
            new ReservationFormResponseInvalidException(
                ReservationFormResponseViolation.MISSING_REQUIRED, "allergies"));

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN, List.of())))
        .isInstanceOf(ReservationFormAnswersInvalidException.class);

    verify(tokenService, never()).generate();
    verify(reservationDao, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void rejectsTokenThatDoesNotOwnTheHoldBeforeLockingSlot() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    when(reservationDao.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn("c".repeat(64));

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN, List.of())))
        .isInstanceOf(ReservationConfirmationInvalidException.class);
    verify(timeSlotDao, never()).findByIdForUpdate(any());
  }

  @Test
  void rejectsHoldAtItsExclusiveExpirationBoundary() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    reservation.setHoldExpiresAt(NOW);
    when(reservationDao.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN, List.of())))
        .isInstanceOf(ReservationHoldExpiredException.class);

    verify(timeSlotDao, never()).findByIdForUpdate(any());
    verify(formConfirmationService, never()).validateAndPersist(any(), any(), any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void rejectsExpiredHoldBeforeConfirmingOrConsumingCapacity() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    reservation.setHoldExpiresAt(NOW.minusSeconds(1));
    when(reservationDao.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN, List.of())))
        .isInstanceOf(ReservationHoldExpiredException.class);

    verify(timeSlotDao, never()).findByIdForUpdate(any());
    verify(reservationDao, never()).sumOccupiedCapacityExcluding(any(), any(), any());
    verify(formConfirmationService, never()).validateAndPersist(any(), any(), any(), any());
    verify(reservationDao, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void invalidTokenDoesNotRevealThatHoldIsExpired() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    reservation.setHoldExpiresAt(NOW.minusSeconds(1));
    when(reservationDao.findByIdForUpdate(reservationId)).thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn("c".repeat(64));

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN, List.of())))
        .isInstanceOf(ReservationConfirmationInvalidException.class);

    verify(timeSlotDao, never()).findByIdForUpdate(any());
    verify(formConfirmationService, never()).validateAndPersist(any(), any(), any(), any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void rejectsWhenOtherOccupantsNoLongerLeaveEnoughCapacity() {
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = reservation(reservationId);
    arrangeValidHold(reservation);
    when(reservationDao.sumOccupiedCapacityExcluding(
            reservation.getTimeSlot().getId(), reservationId, NOW))
        .thenReturn(3L);

    assertThatThrownBy(() -> service.confirm(reservationId, request(TOKEN, List.of())))
        .isInstanceOf(ReservationCapacityUnavailableException.class);
    verify(formConfirmationService, never()).validateAndPersist(any(), any(), any(), any());
  }

  private void arrangeValidHold(ReservationEntity reservation) {
    when(reservationDao.findByIdForUpdate(reservation.getId()))
        .thenReturn(Optional.of(reservation));
    when(tokenService.isValid(TOKEN)).thenReturn(true);
    when(tokenService.hash(TOKEN)).thenReturn(TOKEN_HASH);
    when(timeSlotDao.findByIdForUpdate(reservation.getTimeSlot().getId()))
        .thenReturn(Optional.of(reservation.getTimeSlot()));
    when(reservationDao.sumOccupiedCapacityExcluding(
            reservation.getTimeSlot().getId(), reservation.getId(), NOW))
        .thenReturn(2L);
  }

  private ReservationConfirmRequest request(
      String token, List<ReservationConfirmFormResponse> responses) {
    return new ReservationConfirmRequest(
        token, "  María López  ", "  Maria@Example.COM  ", "en", 2, responses, true, true);
  }

  private ReservationEntity reservation(UUID reservationId) {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setName("Local de prueba");
    venue.setDefaultLocale("es");
    UserEntity owner = new UserEntity();
    owner.setEmail("owner@example.com");
    venue.setOwnerUser(owner);
    venue.setAddress("Calle Mayor 1");
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
