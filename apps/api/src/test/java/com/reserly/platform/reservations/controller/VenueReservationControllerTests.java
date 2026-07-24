package com.reserly.platform.reservations.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.reservations.converter.VenueReservationConverter;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.service.VenueReservationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/** Verifica que el adaptador REST usa el principal y no expone secretos persistidos. */
@ExtendWith(MockitoExtension.class)
class VenueReservationControllerTests {

  @Mock private VenueReservationService reservationService;

  private VenueReservationControllerImpl controller;
  private AuthenticatedAccount account;

  @BeforeEach
  void setUp() {
    controller =
        new VenueReservationControllerImpl(
            reservationService, new VenueReservationConverter());
    account =
        new AuthenticatedAccount(
            UUID.randomUUID(),
            UUID.randomUUID(),
            AccountType.VENUE_BUSINESS,
            "es",
            Set.of("venue_owner"));
  }

  @Test
  void delegatesFiltersAndBuildsPaginatedSummaries() {
    ReservationEntity reservation = reservation();
    UUID timeSlotId = reservation.getTimeSlot().getId();
    LocalDate date = reservation.getDate();
    var page =
        new PageImpl<>(
            List.of(reservation), PageRequest.of(0, 25), 31);
    when(reservationService.list(
            account.userId(),
            "day",
            date,
            timeSlotId,
            "confirmed",
            "ana",
            0,
            25))
        .thenReturn(page);

    var response =
        controller.list(
            account, "day", date, timeSlotId, "confirmed", "ana", 0, 25);

    assertThat(response.getBody().items()).hasSize(1);
    assertThat(response.getBody().totalElements()).isEqualTo(31);
    assertThat(response.getBody().totalPages()).isEqualTo(2);
    assertThat(response.getBody().items().getFirst().customerEmail())
        .isEqualTo("ana@example.com");
    verify(reservationService)
        .list(
            account.userId(),
            "day",
            date,
            timeSlotId,
            "confirmed",
            "ana",
            0,
            25);
  }

  @Test
  void returnsOwnedDetailWithoutManagementCredentials() {
    ReservationEntity reservation = reservation();
    reservation.setSecureTokenHash("secret-hash-that-must-not-be-mapped");
    reservation.setHoldTokenHash("hold-hash-that-must-not-be-mapped");
    when(reservationService.findDetail(account.userId(), reservation.getId()))
        .thenReturn(reservation);

    var response = controller.findDetail(account, reservation.getId());

    assertThat(response.getBody().id()).isEqualTo(reservation.getId());
    assertThat(response.getBody().serviceId()).isEqualTo(reservation.getServiceId());
    assertThat(response.getBody().customerName()).isEqualTo("Ana");
    verify(reservationService).findDetail(account.userId(), reservation.getId());
  }

  @Test
  void mapsFilterAndOwnershipFailuresToStableCodes() {
    VenueReservationExceptionHandler handler = new VenueReservationExceptionHandler();

    assertThat(handler.handleInvalidFilter().getStatusCode().value()).isEqualTo(400);
    assertThat(handler.handleInvalidFilter().getBody().code())
        .isEqualTo("VENUE_RESERVATION_FILTER_INVALID");
    assertThat(handler.handleNotFound().getStatusCode().value()).isEqualTo(404);
    assertThat(handler.handleNotFound().getBody().code())
        .isEqualTo("VENUE_RESERVATION_NOT_FOUND");
  }

  private ReservationEntity reservation() {
    TimeSlotEntity timeSlot = new TimeSlotEntity();
    timeSlot.setId(UUID.randomUUID());
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(UUID.randomUUID());
    reservation.setTimeSlot(timeSlot);
    reservation.setServiceId(UUID.randomUUID());
    reservation.setCustomerName("Ana");
    reservation.setCustomerEmail("ana@example.com");
    reservation.setCustomerEmailNormalized("ana@example.com");
    reservation.setPartySize(2);
    reservation.setDate(LocalDate.of(2026, 7, 24));
    reservation.setStartsAt(LocalTime.of(10, 0));
    reservation.setEndsAt(LocalTime.of(11, 0));
    reservation.setStatus("confirmed");
    reservation.setCreatedAt(Instant.parse("2026-07-20T09:00:00Z"));
    reservation.setUpdatedAt(Instant.parse("2026-07-20T09:05:00Z"));
    return reservation;
  }
}
