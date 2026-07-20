package com.reserly.platform.reservations.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import com.reserly.platform.reservations.service.ReservationConfirmationEmailAnswer;
import com.reserly.platform.reservations.service.ReservationConfirmationEmailRequestedEvent;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

/** Comprueba el sobre persistente y la ruta del trabajo dual de confirmación. */
class ReservationConfirmationEmailEventRelayTests {

  @Test
  void publishesVersionedPersistentJsonWithExactManagementToken() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    var relay = new ReservationConfirmationEmailEventRelay(rabbitTemplate, new ObjectMapper());
    UUID eventId = UUID.randomUUID();
    String token = "B".repeat(43);
    var event =
        new ReservationConfirmationEmailRequestedEvent(
            eventId,
            UUID.randomUUID(),
            "María",
            "maria@example.com",
            "Local",
            "local@example.com",
            "Calle 1",
            "es",
            LocalDate.of(2026, 7, 15),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),
            2,
            "Cancelar con 24 horas",
            token,
            Instant.parse("2026-08-14T10:00:00Z"),
            List.of(
                new ReservationConfirmationEmailAnswer("allergies", "Alergias", "\"Ninguna\"")));

    relay.relay(event);

    ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
    verify(rabbitTemplate)
        .send(
            eq(MessagingTopology.JOBS_EXCHANGE),
            eq(ReservationConfirmationMessagingTopology.ROUTING_KEY),
            message.capture());
    assertThat(message.getValue().getMessageProperties().getMessageId())
        .isEqualTo(eventId.toString());
    assertThat(message.getValue().getMessageProperties().getDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
    assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
        .contains(token)
        .contains("maria@example.com")
        .contains("allergies");
  }
}
