package com.reserly.platform.identity.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.reserly.platform.identity.service.PasswordResetRequestedEvent;
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

/** Verifica el sobre persistente y la ruta del trabajo sensible. */
class PasswordResetEventRelayTests {

  @Test
  void publishesVersionedPersistentJsonWithTheExactToken() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    PasswordResetEventRelay relay = new PasswordResetEventRelay(rabbitTemplate, new ObjectMapper());
    UUID eventId = UUID.randomUUID();
    String token = "b".repeat(43);
    PasswordResetRequestedEvent event =
        new PasswordResetRequestedEvent(
            eventId,
            UUID.randomUUID(),
            "venue@example.com",
            "en",
            token,
            Instant.parse("2026-06-30T12:00:00Z"));

    relay.relay(event);

    ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
    verify(rabbitTemplate)
        .send(
            eq(MessagingTopology.JOBS_EXCHANGE),
            eq(PasswordResetMessagingTopology.ROUTING_KEY),
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
