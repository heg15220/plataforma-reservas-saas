package com.reserly.platform.reservations.messaging;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import com.reserly.platform.reservations.service.VenueReservationCancellationEmailRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Encola el aviso únicamente cuando cancelación y auditoría ya se confirmaron. */
@Component
public class VenueReservationCancellationEmailEventRelay {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(VenueReservationCancellationEmailEventRelay.class);
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public VenueReservationCancellationEmailEventRelay(
      RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  /** El error de broker no revierte una cancelación ya confirmada y se registra sin PII. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void relay(VenueReservationCancellationEmailRequestedEvent event) {
    try {
      Message message =
          MessageBuilder.withBody(objectMapper.writeValueAsBytes(event))
              .setContentType("application/json")
              .setMessageId(event.eventId().toString())
              .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
              .build();
      rabbitTemplate.send(
          MessagingTopology.JOBS_EXCHANGE,
          VenueReservationCancellationMessagingTopology.ROUTING_KEY,
          message);
    } catch (AmqpException | JacksonException exception) {
      LOGGER.error(
          "No se pudo encolar el aviso de cancelación con eventId={}", event.eventId(), exception);
    }
  }
}
