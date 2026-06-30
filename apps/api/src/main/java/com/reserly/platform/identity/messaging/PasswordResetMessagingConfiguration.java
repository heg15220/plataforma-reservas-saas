package com.reserly.platform.identity.messaging;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declara la cola durable y aislada de enlaces de recuperación. */
@Configuration(proxyBeanMethods = false)
public class PasswordResetMessagingConfiguration {

  /** Cola con dead lettering compartido y sin consumidor hasta completar la Fase 8. */
  @Bean
  Queue passwordResetQueue() {
    return QueueBuilder.durable(PasswordResetMessagingTopology.QUEUE)
        .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(MessagingTopology.DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  /** Enlaza la routing key versionada con el exchange de trabajos. */
  @Bean
  Binding passwordResetBinding(Queue passwordResetQueue, TopicExchange jobsExchange) {
    return BindingBuilder.bind(passwordResetQueue)
        .to(jobsExchange)
        .with(PasswordResetMessagingTopology.ROUTING_KEY);
  }
}
