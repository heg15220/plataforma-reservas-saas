package com.reserly.platform.reservations.messaging;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declara una cola durable y aislada para los dos emails derivados de una confirmación. */
@Configuration(proxyBeanMethods = false)
public class ReservationConfirmationMessagingConfiguration {

  /** Conserva trabajos confirmados y deriva rechazos definitivos a la cola compartida. */
  @Bean
  Queue reservationConfirmationEmailQueue() {
    return QueueBuilder.durable(ReservationConfirmationMessagingTopology.QUEUE)
        .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(MessagingTopology.DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  @Bean
  Binding reservationConfirmationEmailBinding(
      Queue reservationConfirmationEmailQueue, TopicExchange jobsExchange) {
    return BindingBuilder.bind(reservationConfirmationEmailQueue)
        .to(jobsExchange)
        .with(ReservationConfirmationMessagingTopology.ROUTING_KEY);
  }
}
