package com.reserly.platform.availability.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.AvailabilityDayRequest;
import com.reserly.platform.availability.persistence.AvailabilityBlockDao;
import com.reserly.platform.availability.persistence.AvailabilityBlockEntity;
import com.reserly.platform.availability.persistence.TimeSlotDao;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica cierres y reservas inactivas por fecha concreta. */
@ExtendWith(MockitoExtension.class)
class AvailabilityDayServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private AvailabilityBlockDao blockDao;
  @Mock private TimeSlotDao slotDao;

  private AvailabilityDayServiceImpl service;
  private UUID ownerId;
  private VenueEntity venue;

  @BeforeEach
  void setUp() {
    service = new AvailabilityDayServiceImpl(venueDao, blockDao, slotDao);
    ownerId = UUID.randomUUID();
    venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
  }

  @Test
  void createsClosedDayAndReservationsDisabledOverrides() {
    LocalDate date = LocalDate.of(2026, 7, 15);
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(blockDao.findOwnedDayOverridesForUpdate(ownerId, date)).thenReturn(List.of());
    when(blockDao.saveAndFlush(any(AvailabilityBlockEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var closed =
        service.replace(ownerId, new AvailabilityDayRequest(date, true, false, "Festivo local"));
    var inactive =
        service.replace(ownerId, new AvailabilityDayRequest(date, false, false, "Equipo interno"));

    assertThat(closed.closed()).isTrue();
    assertThat(closed.reservationsEnabled()).isFalse();
    assertThat(closed.reason()).isEqualTo("Festivo local");
    assertThat(inactive.closed()).isFalse();
    assertThat(inactive.reservationsEnabled()).isFalse();
    assertThat(inactive.reason()).isEqualTo("Equipo interno");
    verify(slotDao, times(2)).markOwnedDayUnavailable(any(), any(), any());
  }

  @Test
  void removesOverrideWhenReservationsAreEnabledAgain() {
    LocalDate date = LocalDate.of(2026, 7, 16);
    AvailabilityBlockEntity existing = new AvailabilityBlockEntity();
    existing.setId(UUID.randomUUID());
    existing.setDate(date);
    existing.setKind("closed_day");
    when(venueDao.findCurrentByOwnerUserIdForUpdate(ownerId)).thenReturn(Optional.of(venue));
    when(blockDao.findOwnedDayOverridesForUpdate(ownerId, date)).thenReturn(List.of(existing));

    var response = service.replace(ownerId, new AvailabilityDayRequest(date, false, true, null));

    assertThat(response.closed()).isFalse();
    assertThat(response.reservationsEnabled()).isTrue();
    assertThat(response.source()).isEqualTo("weekly");
    verify(blockDao).deleteAll(List.of(existing));
    verify(slotDao).reopenOwnedDayUnavailableSlots(any(), any(), any());
  }

  @Test
  void rejectsClosedDayWithReservationsEnabled() {
    assertThatThrownBy(
            () ->
                service.replace(
                    ownerId,
                    new AvailabilityDayRequest(LocalDate.of(2026, 7, 17), true, true, null)))
        .isInstanceOf(AvailabilityDayInvalidException.class);
  }
}
