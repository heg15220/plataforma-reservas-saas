package com.reserly.platform.reservations.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import com.reserly.platform.reservations.service.VenueReservationCancellationEmailRequestedEvent;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import tools.jackson.databind.ObjectMapper;

/** Comprueba el sobre persistente y la ruta aislada del aviso de cancelación. */
class VenueReservationCancellationEmailEventRelayTests {

  @Test
  void publishesPersistentJsonAfterCommitListenerReceivesEvent() {
    RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    var relay =
        new VenueReservationCancellationEmailEventRelay(
            rabbitTemplate, new ObjectMapper());
    var event =
        new VenueReservationCancellationEmailRequestedEvent(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ana@example.com",
            "es",
            "Local",
            "Calle 1",
            LocalDate.of(2026, 8, 1),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            2,
            "Cierre operativo");

    relay.relay(event);

    ArgumentCaptor<Message> message = ArgumentCaptor.forClass(Message.class);
    verify(rabbitTemplate)
        .send(
            eq(MessagingTopology.JOBS_EXCHANGE),
            eq(VenueReservationCancellationMessagingTopology.ROUTING_KEY),
            message.capture());
    assertThat(message.getValue().getMessageProperties().getDeliveryMode())
        .isEqualTo(MessageDeliveryMode.PERSISTENT);
    assertThat(new String(message.getValue().getBody(), StandardCharsets.UTF_8))
        .contains("ana@example.com")
        .contains("Cierre operativo");
  }
}
