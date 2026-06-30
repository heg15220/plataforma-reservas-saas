package com.reserly.platform.identity.messaging;

import com.reserly.platform.identity.service.PasswordResetRequestedEvent;
import com.reserly.platform.infrastructure.messaging.MessagingTopology;
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

/** Publica el enlace sensible solo después de confirmar token y cuenta en PostgreSQL. */
@Component
public class PasswordResetEventRelay {

  private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetEventRelay.class);
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public PasswordResetEventRelay(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  /** Serializa el contrato como mensaje persistente sin registrar su payload. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void relay(PasswordResetRequestedEvent event) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(event);
      Message message =
          MessageBuilder.withBody(payload)
              .setContentType("application/json")
              .setMessageId(event.eventId().toString())
              .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
              .build();
      rabbitTemplate.send(
          MessagingTopology.JOBS_EXCHANGE, PasswordResetMessagingTopology.ROUTING_KEY, message);
    } catch (AmqpException | JacksonException exception) {
      LOGGER.error("No se pudo encolar la recuperación con eventId={}", event.eventId(), exception);
    }
  }
}
