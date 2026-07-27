package com.reserly.platform.reservations.messaging;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cola durable aislada para avisos de cancelación enviados después del commit. */
@Configuration(proxyBeanMethods = false)
public class VenueReservationCancellationMessagingConfiguration {

  @Bean
  Queue venueReservationCancellationEmailQueue() {
    return QueueBuilder.durable(VenueReservationCancellationMessagingTopology.QUEUE)
        .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(MessagingTopology.DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  @Bean
  Binding venueReservationCancellationEmailBinding(
      Queue venueReservationCancellationEmailQueue, TopicExchange jobsExchange) {
    return BindingBuilder.bind(venueReservationCancellationEmailQueue)
        .to(jobsExchange)
        .with(VenueReservationCancellationMessagingTopology.ROUTING_KEY);
  }
}
