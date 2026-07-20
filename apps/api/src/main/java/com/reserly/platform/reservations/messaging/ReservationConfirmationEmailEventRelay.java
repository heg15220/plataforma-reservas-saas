package com.reserly.platform.reservations.messaging;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import com.reserly.platform.reservations.service.ReservationConfirmationEmailRequestedEvent;
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

/** Publica el trabajo persistente solo después de confirmar la transacción PostgreSQL. */
@Component
public class ReservationConfirmationEmailEventRelay {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ReservationConfirmationEmailEventRelay.class);
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public ReservationConfirmationEmailEventRelay(
      RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  /** Serializa un único trabajo idempotente que el consumidor transformará en dos emails. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void relay(ReservationConfirmationEmailRequestedEvent event) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(event);
      Message message =
          MessageBuilder.withBody(payload)
              .setContentType("application/json")
              .setMessageId(event.eventId().toString())
              .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
              .build();
      rabbitTemplate.send(
          MessagingTopology.JOBS_EXCHANGE,
          ReservationConfirmationMessagingTopology.ROUTING_KEY,
          message);
    } catch (AmqpException | JacksonException exception) {
      LOGGER.error(
          "No se pudo encolar el email de confirmación con eventId={}", event.eventId(), exception);
    }
  }
}
