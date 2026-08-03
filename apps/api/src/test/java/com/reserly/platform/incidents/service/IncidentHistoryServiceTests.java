package com.reserly.platform.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.incidents.persistence.NoShowIncidentDao;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/** Cubre propiedad, paginación, derivación del email y conservación operativa. */
class IncidentHistoryServiceTests {

  private static final Instant NOW = Instant.parse("2026-07-27T18:30:00Z");
  private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

  private final ReservationDao reservationDao = mock(ReservationDao.class);
  private final NoShowIncidentDao incidentDao = mock(NoShowIncidentDao.class);
  private final IncidentHistoryService service =
      new IncidentHistoryServiceImpl(reservationDao, incidentDao, Clock.fixed(NOW, ZONE));

  @Test
  void derivesIdentityFromOwnedReservationAndAppliesTwelveMonthCutoff() {
    UUID ownerId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    ReservationEntity reservation = new ReservationEntity();
    reservation.setCustomerEmailNormalized("user@example.com");
    Instant cutoff = NOW.atZone(ZONE).minusMonths(12).toInstant();
    when(reservationDao.findAccessibleDetail(ownerId, reservationId))
        .thenReturn(Optional.of(reservation));
    when(incidentDao.findOperationalHistory("user@example.com", cutoff, PageRequest.of(1, 25)))
        .thenReturn(Page.empty());

    assertThat(service.find(ownerId, reservationId, 1, 25)).isEmpty();
    verify(incidentDao).findOperationalHistory("user@example.com", cutoff, PageRequest.of(1, 25));
  }

  @Test
  void keepsForeignReservationOpaqueAndRejectsUnboundedPage() {
    UUID ownerId = UUID.randomUUID();
    UUID reservationId = UUID.randomUUID();
    when(reservationDao.findAccessibleDetail(ownerId, reservationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.find(ownerId, reservationId, 0, 25))
        .isInstanceOf(IncidentHistoryNotFoundException.class);
    assertThatThrownBy(() -> service.find(ownerId, reservationId, 0, 51))
        .isInstanceOf(IncidentHistoryInvalidException.class);
    verifyNoInteractions(incidentDao);
  }
}
