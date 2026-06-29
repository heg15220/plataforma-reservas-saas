package com.reserly.platform.identity.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.reserly.platform.identity.service.EmailVerificationRequestedEvent;
import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

/** Comprueba el sobre persistente y la ruta del trabajo de entrega. */
class EmailVerificationEventRelayTests {

  @Test
  void publishesVersionedPersistentJsonWithoutChangingTheSecret() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    EmailVerificationEventRelay relay =
        new EmailVerificationEventRelay(rabbitTemplate, new ObjectMapper());
    UUID eventId = UUID.randomUUID();
    String token = "a".repeat(43);
    EmailVerificationRequestedEvent event =
        new EmailVerificationRequestedEvent(
            eventId,
            UUID.randomUUID(),
            "venue@example.com",
            "es",
            token,
            Instant.parse("2026-06-30T12:00:00Z"));

    relay.relay(event);

    ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
    verify(rabbitTemplate)
        .send(
            eq(MessagingTopology.JOBS_EXCHANGE),
            eq(EmailVerificationMessagingTopology.ROUTING_KEY),
            message.capture());
    assertThat(message.getValue().getMessageProperties().getMessageId())
        .isEqualTo(eventId.toString());
    assertThat(message.getValue().getMessageProperties().getDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
    assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
        .contains(token)
        .contains(eventId.toString());
  }
}
