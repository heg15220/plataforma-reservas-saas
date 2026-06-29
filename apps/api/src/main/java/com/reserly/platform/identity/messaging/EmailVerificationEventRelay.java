package com.reserly.platform.identity.messaging;

import com.reserly.platform.identity.service.EmailVerificationRequestedEvent;
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

/**
 * Publica el trabajo solo después de confirmar usuario y token en PostgreSQL.
 *
 * <p>La tarea 8.7 añadirá outbox, reintento operativo y consumidor. Hasta entonces un fallo del
 * broker se registra sin filtrar destinatario ni token y el titular puede solicitar otro desafío.
 */
@Component
public class EmailVerificationEventRelay {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmailVerificationEventRelay.class);
  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper objectMapper;

  public EmailVerificationEventRelay(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
    this.rabbitTemplate = rabbitTemplate;
    this.objectMapper = objectMapper;
  }

  /** Convierte el evento sensible a JSON persistente y lo enruta tras el commit. */
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void relay(EmailVerificationRequestedEvent event) {
    try {
      byte[] payload = objectMapper.writeValueAsBytes(event);
      Message message =
          MessageBuilder.withBody(payload)
              .setContentType("application/json")
              .setMessageId(event.eventId().toString())
              .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
              .build();
      rabbitTemplate.send(
          MessagingTopology.JOBS_EXCHANGE, EmailVerificationMessagingTopology.ROUTING_KEY, message);
    } catch (AmqpException | JacksonException exception) {
      LOGGER.error(
          "No se pudo encolar la verificación de email con eventId={}", event.eventId(), exception);
    }
  }
}
