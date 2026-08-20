package com.reserly.platform.demand.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.demand.attribution.BookingAttributionRequestedEvent;
import com.reserly.platform.demand.correlation.DemandCorrelationContext;
import com.reserly.platform.demand.ingestion.DemandEventIngestionService;
import com.reserly.platform.incidents.dto.AttendanceUpdateRequest;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import com.reserly.platform.venues.persistence.VenueEntity;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** Verifica cobertura del catálogo operativo y aislamiento ante caída de persistencia. */
class DemandOperationalTelemetryTests {

  private static final Instant NOW = Instant.parse("2026-08-13T13:00:00Z");

  @Test
  void publishesAllCanonicalBackendOutcomesWithoutPersonalData() {
    ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    DemandCorrelationContext correlation = mock(DemandCorrelationContext.class);
    UUID correlationId = UUID.randomUUID();
    when(correlation.currentOrNew()).thenReturn(correlationId);
    DemandOperationalTelemetryAspect aspect =
        new DemandOperationalTelemetryAspect(
            publisher, Clock.fixed(NOW, ZoneOffset.UTC), correlation);
    UUID reservationId = UUID.randomUUID();
    UUID venueId = UUID.randomUUID();

    aspect.availabilityChecked(
        "venue-slug",
        LocalDate.of(2026, 8, 14),
        new PublicVenueAvailabilityResponse(
            "venue-slug",
            LocalDate.of(2026, 8, 14),
            5,
            "available",
            "Available",
            true,
            false,
            true,
            "schedule",
            3,
            List.of()));
    aspect.bookingStarted(
        new ReservationHoldRequest(venueId, UUID.randomUUID(), null, null, null, 2),
        new ReservationHoldResponse(reservationId, "secret-not-forwarded", NOW, 300));
    aspect.bookingCompleted(
        new ReservationConfirmResponse(
            "confirmed",
            reservationId,
            "private@example.invalid",
            "Venue",
            LocalDate.now(),
            LocalTime.NOON,
            LocalTime.NOON.plusHours(1),
            2));
    aspect.bookingCancelledByCustomer();
    aspect.bookingCancelledByVenue(reservation(reservationId, venueId));
    aspect.attendanceUpdated(
        new AttendanceUpdateRequest("attended"), reservation(reservationId, venueId));
    NoShowIncidentEntity incident = new NoShowIncidentEntity();
    incident.setReservationId(reservationId);
    incident.setVenueId(venueId);
    aspect.noShowReported(incident);
    aspect.reviewSubmitted(
        new ReviewCreateResponse(
            "created", UUID.randomUUID(), venueId, reservationId, 5, BigDecimal.valueOf(4.8), 10));

    ArgumentCaptor<Object> published = ArgumentCaptor.forClass(Object.class);
    verify(publisher, org.mockito.Mockito.times(9)).publishEvent(published.capture());
    List<DemandTelemetryEvent> events =
        published.getAllValues().stream()
            .filter(DemandTelemetryEvent.class::isInstance)
            .map(DemandTelemetryEvent.class::cast)
            .toList();
    assertThat(events)
        .extracting(DemandTelemetryEvent::eventType)
        .containsExactly(
            "availabilityChecked",
            "bookingStarted",
            "bookingCompleted",
            "bookingCancelled",
            "bookingCancelled",
            "attendanceConfirmed",
            "noShow",
            "reviewSubmitted");
    assertThat(events).extracting(DemandTelemetryEvent::requestId).containsOnly(correlationId);
    assertThat(published.getAllValues())
        .filteredOn(BookingAttributionRequestedEvent.class::isInstance)
        .singleElement()
        .satisfies(
            event -> {
              BookingAttributionRequestedEvent attribution =
                  (BookingAttributionRequestedEvent) event;
              assertThat(attribution.reservationId()).isEqualTo(reservationId);
              assertThat(attribution.requestId()).isEqualTo(correlationId);
              assertThat(attribution.confirmedAt()).isEqualTo(NOW);
            });
    assertThat(published.getAllValues().toString()).doesNotContain("private@example.invalid");
  }

  @Test
  void listenerSwallowsIngestionFailureAndCountsDrop() {
    DemandEventIngestionService ingestion = mock(DemandEventIngestionService.class);
    doThrow(new IllegalStateException("database unavailable")).when(ingestion).ingestTrusted(any());
    SimpleMeterRegistry meters = new SimpleMeterRegistry();
    DemandTelemetryEventListener listener = new DemandTelemetryEventListener(ingestion, meters);

    listener.record(
        DemandTelemetryEvent.create(
            "bookingCompleted",
            NOW,
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            java.util.Map.of("outcomeCode", "confirmed")));

    assertThat(meters.get("reserly.demand.telemetry.dropped").counter().count()).isEqualTo(1);
  }

  private ReservationEntity reservation(UUID reservationId, UUID venueId) {
    ReservationEntity reservation = mock(ReservationEntity.class);
    VenueEntity venue = mock(VenueEntity.class);
    TimeSlotEntity slot = mock(TimeSlotEntity.class);
    when(reservation.getId()).thenReturn(reservationId);
    when(reservation.getVenue()).thenReturn(venue);
    when(venue.getId()).thenReturn(venueId);
    when(reservation.getTimeSlot()).thenReturn(slot);
    when(slot.getId()).thenReturn(UUID.randomUUID());
    return reservation;
  }
}
