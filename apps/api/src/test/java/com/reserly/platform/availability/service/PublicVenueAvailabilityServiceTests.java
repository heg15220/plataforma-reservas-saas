package com.reserly.platform.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.persistence.AvailabilityBlockDao;
import com.reserly.platform.availability.persistence.AvailabilityBlockEntity;
import com.reserly.platform.availability.persistence.TimeSlotDao;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.availability.persistence.VenueOpeningHourDao;
import com.reserly.platform.availability.persistence.VenueOpeningHourEntity;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica el cálculo público de estado operativo y franjas por fecha. */
@ExtendWith(MockitoExtension.class)
class PublicVenueAvailabilityServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenueOpeningHourDao openingHourDao;
  @Mock private AvailabilityBlockDao blockDao;
  @Mock private TimeSlotDao slotDao;

  private PublicVenueAvailabilityServiceImpl service;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new PublicVenueAvailabilityServiceImpl(venueDao, openingHourDao, blockDao, slotDao);
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setSlug("casa-luz");
  }

  @Test
  void returnsOpenWhenPublishedVenueHasAvailableSlots() {
    LocalDate date = LocalDate.of(2026, 7, 13);
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(slotDao.findPublishedByVenueIdAndDate(venue.getId(), date))
        .thenReturn(List.of(slot(date, "available", 6), slot(date, "blocked", 4)));
    when(blockDao.findPublishedDayOverride(venue.getId(), date)).thenReturn(Optional.empty());
    when(openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), 1))
        .thenReturn(Optional.of(openingHour(false, true)));

    var response = service.findBySlug("casa-luz", date, SupportedLocale.ES);

    assertThat(response.statusCode()).isEqualTo("open");
    assertThat(response.statusLabel()).isEqualTo("Abierto");
    assertThat(response.bookingAvailable()).isTrue();
    assertThat(response.availableSlotCount()).isEqualTo(1);
    assertThat(response.slots()).hasSize(2);
    assertThat(response.slots().get(0).availableCapacity()).isEqualTo(6);
    assertThat(response.slots().get(1).bookingAvailable()).isFalse();
  }

  @Test
  void returnsClosedWhenDayHasClosedOverride() {
    LocalDate date = LocalDate.of(2026, 7, 15);
    AvailabilityBlockEntity block = new AvailabilityBlockEntity();
    block.setKind("closed_day");
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(slotDao.findPublishedByVenueIdAndDate(venue.getId(), date))
        .thenReturn(List.of(slot(date, "unavailable", 3)));
    when(blockDao.findPublishedDayOverride(venue.getId(), date)).thenReturn(Optional.of(block));
    when(openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), 3))
        .thenReturn(Optional.of(openingHour(false, true)));

    var response = service.findBySlug("casa-luz", date, SupportedLocale.EN);

    assertThat(response.statusCode()).isEqualTo("closed");
    assertThat(response.statusLabel()).isEqualTo("Closed");
    assertThat(response.closed()).isTrue();
    assertThat(response.bookingAvailable()).isFalse();
    assertThat(response.source()).isEqualTo("override");
  }

  @Test
  void returnsUpcomingAvailableWhenDateHasNoBookableSlotsButFutureExists() {
    LocalDate date = LocalDate.of(2026, 7, 16);
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(slotDao.findPublishedByVenueIdAndDate(venue.getId(), date)).thenReturn(List.of());
    when(blockDao.findPublishedDayOverride(venue.getId(), date)).thenReturn(Optional.empty());
    when(openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), 4))
        .thenReturn(Optional.of(openingHour(false, true)));
    when(slotDao.existsPublishedAvailableAfter(venue.getId(), date)).thenReturn(true);

    var response = service.findBySlug("casa-luz", date, SupportedLocale.ES);

    assertThat(response.statusCode()).isEqualTo("upcoming_available");
    assertThat(response.statusLabel()).isEqualTo("Próximamente disponible");
    assertThat(response.bookingAvailable()).isFalse();
    assertThat(response.source()).isEqualTo("future_slots");
  }

  @Test
  void returnsClosedWhenWeeklyScheduleIsClosed() {
    LocalDate date = LocalDate.of(2026, 7, 14);
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(slotDao.findPublishedByVenueIdAndDate(venue.getId(), date))
        .thenReturn(List.of(slot(date, "available", 2)));
    when(blockDao.findPublishedDayOverride(venue.getId(), date)).thenReturn(Optional.empty());
    when(openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), 2))
        .thenReturn(Optional.of(openingHour(true, false)));

    var response = service.findBySlug("casa-luz", date, SupportedLocale.ES);

    assertThat(response.statusCode()).isEqualTo("closed");
    assertThat(response.closed()).isTrue();
    assertThat(response.reservationsEnabled()).isFalse();
    assertThat(response.source()).isEqualTo("weekly");
  }

  @Test
  void returnsUnavailableWhenReservationsAreDisabledByOverride() {
    LocalDate date = LocalDate.of(2026, 7, 17);
    AvailabilityBlockEntity block = new AvailabilityBlockEntity();
    block.setKind("reservations_disabled");
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(slotDao.findPublishedByVenueIdAndDate(venue.getId(), date))
        .thenReturn(List.of(slot(date, "available", 2)));
    when(blockDao.findPublishedDayOverride(venue.getId(), date)).thenReturn(Optional.of(block));
    when(openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), 5))
        .thenReturn(Optional.of(openingHour(false, true)));

    var response = service.findBySlug("casa-luz", date, SupportedLocale.ES);

    assertThat(response.statusCode()).isEqualTo("unavailable");
    assertThat(response.bookingAvailable()).isFalse();
    assertThat(response.closed()).isFalse();
    assertThat(response.reservationsEnabled()).isFalse();
    assertThat(response.source()).isEqualTo("override");
  }

  @Test
  void returnsFullWhenEveryConfiguredSlotIsFull() {
    LocalDate date = LocalDate.of(2026, 7, 18);
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(slotDao.findPublishedByVenueIdAndDate(venue.getId(), date))
        .thenReturn(List.of(slot(date, "full", 1), slot(date, "full", 3)));
    when(blockDao.findPublishedDayOverride(venue.getId(), date)).thenReturn(Optional.empty());
    when(openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), 6))
        .thenReturn(Optional.of(openingHour(false, true)));

    var response = service.findBySlug("casa-luz", date, SupportedLocale.ES);

    assertThat(response.statusCode()).isEqualTo("full");
    assertThat(response.statusLabel()).isEqualTo("Completo");
    assertThat(response.bookingAvailable()).isFalse();
    assertThat(response.reservationsEnabled()).isTrue();
    assertThat(response.availableSlotCount()).isZero();
  }

  @Test
  void returnsUnavailableWhenThereAreNoSlotsAndNoFutureAvailability() {
    LocalDate date = LocalDate.of(2026, 7, 19);
    when(venueDao.findPublishedBySlug("casa-luz")).thenReturn(Optional.of(venue));
    when(slotDao.findPublishedByVenueIdAndDate(venue.getId(), date)).thenReturn(List.of());
    when(blockDao.findPublishedDayOverride(venue.getId(), date)).thenReturn(Optional.empty());
    when(openingHourDao.findPublishedByVenueIdAndWeekday(venue.getId(), 7))
        .thenReturn(Optional.of(openingHour(false, true)));
    when(slotDao.existsPublishedAvailableAfter(venue.getId(), date)).thenReturn(false);

    var response = service.findBySlug("casa-luz", date, null);

    assertThat(response.statusCode()).isEqualTo("unavailable");
    assertThat(response.statusLabel()).isEqualTo("Unavailable");
    assertThat(response.source()).isEqualTo("slots");
    assertThat(response.bookingAvailable()).isFalse();
  }

  @Test
  void rejectsMissingDateAndUnpublishedVenue() {
    assertThatThrownBy(() -> service.findBySlug("casa-luz", null, SupportedLocale.ES))
        .isInstanceOf(TimeSlotInvalidException.class);
    when(venueDao.findPublishedBySlug("borrador")).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.findBySlug("borrador", LocalDate.of(2026, 7, 13), SupportedLocale.ES))
        .isInstanceOf(VenueProfileNotFoundException.class);
  }

  private static VenueOpeningHourEntity openingHour(boolean closed, boolean reservationsEnabled) {
    VenueOpeningHourEntity openingHour = new VenueOpeningHourEntity();
    openingHour.setClosed(closed);
    openingHour.setReservationsEnabled(reservationsEnabled);
    openingHour.setOpensAt(LocalTime.of(9, 0));
    openingHour.setClosesAt(LocalTime.of(17, 0));
    return openingHour;
  }

  private static TimeSlotEntity slot(LocalDate date, String status, int capacity) {
    TimeSlotEntity slot = new TimeSlotEntity();
    slot.setId(UUID.randomUUID());
    slot.setDate(date);
    slot.setStartsAt(LocalTime.of(10, 0));
    slot.setEndsAt(LocalTime.of(11, 0));
    slot.setCapacity(capacity);
    slot.setStatus(status);
    return slot;
  }
}
