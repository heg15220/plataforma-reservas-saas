package com.reserly.platform.reservations.messaging;

import com.reserly.platform.notifications.EmailDeliveryDao;
import com.reserly.platform.notifications.EmailDeliveryEntity;
import com.reserly.platform.notifications.LocalizedEmailTemplateService;
import com.reserly.platform.notifications.RenderedEmailTemplate;
import com.reserly.platform.notifications.ReservationConfirmationTemplateData;
import com.reserly.platform.notifications.TransactionalEmailMessage;
import com.reserly.platform.notifications.TransactionalEmailProvider;
import com.reserly.platform.notifications.VenueReservationNotificationTemplateData;
import com.reserly.platform.reservations.service.ReservationConfirmationEmailRequestedEvent;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.LockSupport;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Consume confirmaciones con idempotencia independiente para cliente y local.
 *
 * <p>Los fallos de procesamiento se limitan con backoff antes del rechazo definitivo. Un payload
 * inválido se rechaza definitivamente sin registrar su cuerpo.
 */
@Component
public class ReservationConfirmationEmailConsumer {

  private static final int MAX_ATTEMPTS = 3;

  private final ObjectMapper objectMapper;
  private final LocalizedEmailTemplateService templates;
  private final TransactionalEmailProvider provider;
  private final EmailDeliveryDao deliveryDao;
  private final Clock clock;
  private final String webBaseUrl;

  public ReservationConfirmationEmailConsumer(
      ObjectMapper objectMapper,
      LocalizedEmailTemplateService templates,
      TransactionalEmailProvider provider,
      EmailDeliveryDao deliveryDao,
      Clock clock,
      @Value("${reserly.webPublicBaseUrl}") String webBaseUrl) {
    this.objectMapper = objectMapper;
    this.templates = templates;
    this.provider = provider;
    this.deliveryDao = deliveryDao;
    this.clock = clock;
    this.webBaseUrl = webBaseUrl;
  }

  /** Procesa un trabajo persistente; nunca escribe payload, destinatarios o token en logs. */
  @RabbitListener(queues = ReservationConfirmationMessagingTopology.QUEUE)
  public void consume(Message message) {
    ReservationConfirmationEmailRequestedEvent event = deserialize(message);
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        deliver(event, "customer", event.customerEmail(), customerTemplate(event));
        deliver(event, "venue", event.venueEmail(), venueTemplate(event));
        return;
      } catch (RuntimeException exception) {
        if (attempt == MAX_ATTEMPTS) {
          throw new AmqpRejectAndDontRequeueException(
              "Reservation email attempts exhausted", exception);
        }
        LockSupport.parkNanos(attempt * 1_000_000_000L);
        if (Thread.currentThread().isInterrupted()) {
          throw new AmqpRejectAndDontRequeueException(
              "Reservation email retry interrupted", exception);
        }
      }
    }
  }

  private ReservationConfirmationEmailRequestedEvent deserialize(Message message) {
    try {
      return objectMapper.readValue(
          message.getBody(), ReservationConfirmationEmailRequestedEvent.class);
    } catch (JacksonException exception) {
      throw new AmqpRejectAndDontRequeueException("Invalid reservation email event", exception);
    }
  }

  private RenderedEmailTemplate customerTemplate(ReservationConfirmationEmailRequestedEvent event) {
    var answers =
        event.formResponses().stream()
            .map(
                answer ->
                    new ReservationConfirmationTemplateData.Answer(
                        answer.label(), answer.valueJson()))
            .toList();
    return templates.renderReservationConfirmation(
        event.locale(),
        new ReservationConfirmationTemplateData(
            event.venueName(),
            event.venueAddress(),
            event.date(),
            event.startsAt(),
            event.endsAt(),
            event.partySize(),
            event.bookingRules() == null ? "" : event.bookingRules(),
            URI.create(webBaseUrl + "/reservas/gestionar/" + event.manageToken()),
            event.manageTokenExpiresAt(),
            answers));
  }

  private RenderedEmailTemplate venueTemplate(ReservationConfirmationEmailRequestedEvent event) {
    var answers =
        event.formResponses().stream()
            .map(
                answer ->
                    new VenueReservationNotificationTemplateData.Answer(
                        answer.label(), answer.valueJson()))
            .toList();
    return templates.renderVenueReservationNotification(
        event.locale(),
        new VenueReservationNotificationTemplateData(
            event.venueName(),
            event.customerName(),
            event.customerEmail(),
            event.date(),
            event.startsAt(),
            event.endsAt(),
            event.partySize(),
            answers));
  }

  private void deliver(
      ReservationConfirmationEmailRequestedEvent event,
      String recipientKind,
      String recipient,
      RenderedEmailTemplate template) {
    EmailDeliveryEntity delivery =
        deliveryDao
            .findByEventIdAndRecipientKind(event.eventId(), recipientKind)
            .orElseGet(() -> newDelivery(event.eventId(), event.reservationId(), recipientKind));
    if ("delivered".equals(delivery.getStatus())) {
      return;
    }
    delivery.setAttemptCount(delivery.getAttemptCount() + 1);
    delivery.setUpdatedAt(clock.instant());
    deliveryDao.save(delivery);
    try {
      provider.send(
          new TransactionalEmailMessage(
              recipient, template.subject(), template.textBody(), template.htmlBody()));
      delivery.setStatus("delivered");
      delivery.setDeliveredAt(clock.instant());
      delivery.setLastErrorCode(null);
      deliveryDao.save(delivery);
    } catch (RuntimeException exception) {
      delivery.setStatus("failed");
      delivery.setLastErrorCode("PROVIDER_REJECTED");
      delivery.setUpdatedAt(clock.instant());
      deliveryDao.save(delivery);
      throw exception;
    }
  }

  private EmailDeliveryEntity newDelivery(UUID eventId, UUID reservationId, String recipientKind) {
    Instant now = clock.instant();
    EmailDeliveryEntity delivery = new EmailDeliveryEntity();
    delivery.setEventId(eventId);
    delivery.setReservationId(reservationId);
    delivery.setRecipientKind(recipientKind);
    delivery.setStatus("pending");
    delivery.setCreatedAt(now);
    delivery.setUpdatedAt(now);
    return delivery;
  }
}
