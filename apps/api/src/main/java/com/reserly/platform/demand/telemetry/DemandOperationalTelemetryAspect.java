package com.reserly.platform.demand.telemetry;

import com.reserly.platform.availability.dto.PublicVenueAvailabilityResponse;
import com.reserly.platform.incidents.dto.AttendanceUpdateRequest;
import com.reserly.platform.incidents.persistence.NoShowIncidentEntity;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import com.reserly.platform.reservations.dto.ReservationHoldRequest;
import com.reserly.platform.reservations.dto.ReservationHoldResponse;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reviews.dto.ReviewCreateResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Observa exclusivamente resultados exitosos de servicios operativos sin alterar sus firmas.
 *
 * <p>El aspecto envuelve al interceptor transaccional; por ello publica después de un commit
 * correcto. El listener asíncrono absorbe fallos y mantiene reserva/disponibilidad independientes.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class DemandOperationalTelemetryAspect {

  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public DemandOperationalTelemetryAspect(ApplicationEventPublisher eventPublisher, Clock clock) {
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  /** Registra cada consulta de disponibilidad ya calculada por Spring. */
  @AfterReturning(
      pointcut =
          "execution(* com.reserly.platform.availability.service.PublicVenueAvailabilityServiceImpl.findBySlug(..)) && args(slug,date,..)",
      returning = "response")
  public void availabilityChecked(
      String slug, LocalDate date, PublicVenueAvailabilityResponse response) {
    publish(
        "availabilityChecked",
        UUID.randomUUID(),
        null,
        null,
        null,
        null,
        Map.of(
            "availabilityDate", date.toString(),
            "availableSlotCount", response.availableSlotCount()));
  }

  /** Registra un hold creado como inicio canónico del intento de reserva. */
  @AfterReturning(
      pointcut =
          "execution(* com.reserly.platform.reservations.service.ReservationHoldServiceImpl.create(..)) && args(request)",
      returning = "response")
  public void bookingStarted(ReservationHoldRequest request, ReservationHoldResponse response) {
    publish(
        "bookingStarted",
        response.reservationId(),
        request.venueId(),
        request.serviceId(),
        null,
        request.timeSlotId(),
        Map.of("stepCode", "holdCreated"));
  }

  /** Registra únicamente una confirmación ya comprometida. */
  @AfterReturning(
      pointcut =
          "execution(* com.reserly.platform.reservations.service.ReservationConfirmationServiceImpl.confirm(..))",
      returning = "response")
  public void bookingCompleted(ReservationConfirmResponse response) {
    publish(
        "bookingCompleted",
        response.reservationId(),
        null,
        null,
        null,
        null,
        Map.of("outcomeCode", "confirmed"));
  }

  /** Registra cancelación del cliente sin observar ni conservar su token. */
  @AfterReturning(
      "execution(* com.reserly.platform.reservations.service.ReservationManagementServiceImpl.cancelByToken(..))")
  public void bookingCancelledByCustomer() {
    publish(
        "bookingCancelled",
        UUID.randomUUID(),
        null,
        null,
        null,
        null,
        Map.of("outcomeCode", "cancelledByUser"));
  }

  /** Registra cancelación del local usando solo IDs técnicos del agregado. */
  @AfterReturning(
      pointcut =
          "execution(* com.reserly.platform.reservations.service.VenueReservationCancellationServiceImpl.cancel(..))",
      returning = "reservation")
  public void bookingCancelledByVenue(ReservationEntity reservation) {
    publishReservation("bookingCancelled", reservation, Map.of("outcomeCode", "cancelledByVenue"));
  }

  /** Registra asistencia confirmada; el estado no-show se registra al crear su incidencia. */
  @AfterReturning(
      pointcut =
          "execution(* com.reserly.platform.incidents.service.AttendanceServiceImpl.update(..)) && args(*,*,request)",
      returning = "reservation")
  public void attendanceUpdated(AttendanceUpdateRequest request, ReservationEntity reservation) {
    if ("attended".equalsIgnoreCase(request.status())) {
      publishReservation("attendanceConfirmed", reservation, Map.of("outcomeCode", "attended"));
    }
  }

  /** Registra el no-show solo después de crear incidencia, auditoría y penalización. */
  @AfterReturning(
      pointcut =
          "execution(* com.reserly.platform.incidents.service.NoShowReportServiceImpl.report(..))",
      returning = "incident")
  public void noShowReported(NoShowIncidentEntity incident) {
    publish(
        "noShow",
        incident.getReservationId(),
        incident.getVenueId(),
        null,
        null,
        null,
        Map.of("outcomeCode", "reported"));
  }

  /** Registra rating estructurado sin comentario ni email de la reseña. */
  @AfterReturning(
      pointcut =
          "execution(* com.reserly.platform.reviews.service.ReviewCreationServiceImpl.create*(..))",
      returning = "response")
  public void reviewSubmitted(ReviewCreateResponse response) {
    publish(
        "reviewSubmitted",
        response.reservationId(),
        response.venueId(),
        null,
        null,
        null,
        Map.of("outcomeCode", "submitted", "rating", response.rating()));
  }

  private void publishReservation(
      String eventType, ReservationEntity reservation, Map<String, Object> context) {
    publish(
        eventType,
        reservation.getId(),
        reservation.getVenue().getId(),
        reservation.getServiceId(),
        reservation.getEmployeeResourceId(),
        reservation.getTimeSlot().getId(),
        context);
  }

  private void publish(
      String eventType,
      UUID requestId,
      UUID venueId,
      UUID serviceId,
      UUID resourceId,
      UUID timeSlotId,
      Map<String, Object> context) {
    eventPublisher.publishEvent(
        DemandTelemetryEvent.create(
            eventType,
            clock.instant(),
            requestId,
            venueId,
            serviceId,
            resourceId,
            timeSlotId,
            context));
  }
}
