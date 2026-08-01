package com.reserly.platform.reservations.messaging;

import com.reserly.platform.notifications.EmailDeliveryDao;
import com.reserly.platform.notifications.EmailDeliveryEntity;
import com.reserly.platform.notifications.LocalizedEmailTemplateService;
import com.reserly.platform.notifications.ReservationCancelledByVenueTemplateData;
import com.reserly.platform.notifications.TransactionalEmailMessage;
import com.reserly.platform.notifications.TransactionalEmailProvider;
import com.reserly.platform.reservations.service.VenueReservationCancellationEmailRequestedEvent;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.locks.LockSupport;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Entrega de forma idempotente el aviso localizado de cancelación al cliente.
 *
 * <p>Reintenta tres veces con espera corta y persiste solo metadatos operativos, nunca
 * destinatario, motivo o cuerpo.
 */
@Component
public class VenueReservationCancellationEmailConsumer {

  private static final int MAX_ATTEMPTS = 3;
  private static final String RECIPIENT_KIND = "customer";

  private final ObjectMapper objectMapper;
  private final LocalizedEmailTemplateService templates;
  private final TransactionalEmailProvider provider;
  private final EmailDeliveryDao deliveryDao;
  private final Clock clock;

  public VenueReservationCancellationEmailConsumer(
      ObjectMapper objectMapper,
      LocalizedEmailTemplateService templates,
      TransactionalEmailProvider provider,
      EmailDeliveryDao deliveryDao,
      Clock clock) {
    this.objectMapper = objectMapper;
    this.templates = templates;
    this.provider = provider;
    this.deliveryDao = deliveryDao;
    this.clock = clock;
  }

  @RabbitListener(queues = VenueReservationCancellationMessagingTopology.QUEUE)
  public void consume(Message message) {
    VenueReservationCancellationEmailRequestedEvent event = deserialize(message);
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        deliver(event);
        return;
      } catch (RuntimeException exception) {
        if (attempt == MAX_ATTEMPTS) {
          throw new AmqpRejectAndDontRequeueException(
              "Venue cancellation email attempts exhausted", exception);
        }
        LockSupport.parkNanos(attempt * 1_000_000_000L);
        if (Thread.currentThread().isInterrupted()) {
          throw new AmqpRejectAndDontRequeueException(
              "Venue cancellation email retry interrupted", exception);
        }
      }
    }
  }

  private VenueReservationCancellationEmailRequestedEvent deserialize(Message message) {
    try {
      return objectMapper.readValue(
          message.getBody(), VenueReservationCancellationEmailRequestedEvent.class);
    } catch (JacksonException exception) {
      throw new AmqpRejectAndDontRequeueException(
          "Invalid venue cancellation email event", exception);
    }
  }

  private void deliver(VenueReservationCancellationEmailRequestedEvent event) {
    EmailDeliveryEntity delivery =
        deliveryDao
            .findByEventIdAndRecipientKind(event.eventId(), RECIPIENT_KIND)
            .orElseGet(() -> newDelivery(event));
    if ("delivered".equals(delivery.getStatus())) {
      return;
    }
    delivery.setAttemptCount(delivery.getAttemptCount() + 1);
    delivery.setUpdatedAt(clock.instant());
    deliveryDao.save(delivery);
    try {
      var rendered =
          templates.renderVenueCancellationNotice(
              event.customerLocale(),
              new ReservationCancelledByVenueTemplateData(
                  event.venueName(),
                  event.venueAddress(),
                  event.date(),
                  event.startsAt(),
                  event.endsAt(),
                  event.partySize(),
                  event.cancellationReason()));
      provider.send(
          new TransactionalEmailMessage(
              event.customerEmail(), rendered.subject(), rendered.textBody(), rendered.htmlBody()));
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

  private EmailDeliveryEntity newDelivery(VenueReservationCancellationEmailRequestedEvent event) {
    Instant now = clock.instant();
    EmailDeliveryEntity delivery = new EmailDeliveryEntity();
    delivery.setEventId(event.eventId());
    delivery.setReservationId(event.reservationId());
    delivery.setRecipientKind(RECIPIENT_KIND);
    delivery.setStatus("pending");
    delivery.setCreatedAt(now);
    delivery.setUpdatedAt(now);
    return delivery;
  }
}
