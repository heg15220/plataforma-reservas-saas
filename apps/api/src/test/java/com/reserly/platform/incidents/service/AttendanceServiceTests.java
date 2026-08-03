package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.incidents.dto.AttendanceUpdateRequest;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.service.ReservationOperationalWindow;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cobertura determinista de la máquina de asistencia, propiedad y frontera temporal. */
class AttendanceServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-27T12:00:00Z");

  private final ReservationDao reservationDao = mock(ReservationDao.class);
  private final Clock clock = Clock.fixed(NOW, ZoneId.of("Europe/Madrid"));
  private final AttendanceService service =
      new AttendanceServiceImpl(reservationDao, clock, new ReservationOperationalWindow(clock));

  @BeforeEach
  void returnSavedReservation() {
    when(reservationDao.saveAndFlush(any(ReservationEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void marksReservationAttendedDuringOperationalHourWithAuditInstant() {
    UUID ownerId = UUID.randomUUID();
    ReservationEntity reservation = operationalReservation("confirmed");
    when(reservationDao.findAccessibleForAttendanceUpdate(ownerId, reservation.getId()))
        .thenReturn(Optional.of(reservation));

    ReservationEntity updated =
        service.update(ownerId, reservation.getId(), new AttendanceUpdateRequest(" attended "));

    assertThat(updated.getStatus()).isEqualTo("attended");
    assertThat(updated.getAttendanceMarkedAt()).isEqualTo(NOW);
    assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
    verify(reservationDao).saveAndFlush(reservation);
  }

  @Test
  void marksNoShowWithoutCreatingIncident() {
    UUID ownerId = UUID.randomUUID();
    ReservationEntity reservation = operationalReservation("confirmed");
    when(reservationDao.findAccessibleForAttendanceUpdate(ownerId, reservation.getId()))
        .thenReturn(Optional.of(reservation));

    service.update(ownerId, reservation.getId(), new AttendanceUpdateRequest("no_show"));
    assertThat(reservation.getStatus()).isEqualTo("no_show");
    assertThat(reservation.getAttendanceMarkedAt()).isEqualTo(NOW);
  }

  @Test
  void rejectsReservationBeforeStartWithoutMutation() {
    UUID ownerId = UUID.randomUUID();
    ReservationEntity reservation = operationalReservation("confirmed");
    reservation.setDate(LocalDate.of(2026, 7, 27));
    reservation.setStartsAt(LocalTime.of(15, 0));
    when(reservationDao.findAccessibleForAttendanceUpdate(ownerId, reservation.getId()))
        .thenReturn(Optional.of(reservation));

    assertThatThrownBy(
            () ->
                service.update(
                    ownerId, reservation.getId(), new AttendanceUpdateRequest("attended")))
        .isInstanceOf(AttendanceInvalidException.class);

    assertThat(reservation.getStatus()).isEqualTo("confirmed");
    verify(reservationDao, never()).saveAndFlush(any());
  }

  @Test
  void rejectsForeignReservationAndNonMarkableState() {
    UUID ownerId = UUID.randomUUID();
    UUID missingReservationId = UUID.randomUUID();
    when(reservationDao.findAccessibleForAttendanceUpdate(ownerId, missingReservationId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.update(
                    ownerId, missingReservationId, new AttendanceUpdateRequest("attended")))
        .isInstanceOf(AttendanceNotFoundException.class);

    ReservationEntity cancelled = operationalReservation("cancelled_by_user");
    when(reservationDao.findAccessibleForAttendanceUpdate(ownerId, cancelled.getId()))
        .thenReturn(Optional.of(cancelled));
    assertThatThrownBy(
            () ->
                service.update(ownerId, cancelled.getId(), new AttendanceUpdateRequest("no_show")))
        .isInstanceOf(AttendanceInvalidException.class);
  }

  @Test
  void rejectsReservationWhenOperationalHourHasElapsed() {
    UUID ownerId = UUID.randomUUID();
    ReservationEntity reservation = operationalReservation("confirmed");
    reservation.setStartsAt(LocalTime.of(12, 59));
    when(reservationDao.findAccessibleForAttendanceUpdate(ownerId, reservation.getId()))
        .thenReturn(Optional.of(reservation));

    assertThatThrownBy(
            () ->
                service.update(
                    ownerId, reservation.getId(), new AttendanceUpdateRequest("no_show")))
        .isInstanceOf(AttendanceInvalidException.class);
    verify(reservationDao, never()).saveAndFlush(any());
  }

  private ReservationEntity operationalReservation(String status) {
    ReservationEntity reservation = new ReservationEntity();
    reservation.setId(UUID.randomUUID());
    reservation.setCustomerEmail("customer@example.com");
    reservation.setDate(LocalDate.of(2026, 7, 27));
    reservation.setStartsAt(LocalTime.of(13, 30));
    reservation.setEndsAt(LocalTime.of(14, 30));
    reservation.setStatus(status);
    return reservation;
  }
}
