package com.reserly.platform.identity.messaging;

import com.reserly.platform.infrastructure.messaging.MessagingTopology;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declara una cola durable y aislada para solicitudes de verificación de email. */
@Configuration(proxyBeanMethods = false)
public class EmailVerificationMessagingConfiguration {

  /** Cola durable con salida explícita a aparcamiento ante rechazo definitivo. */
  @Bean
  Queue emailVerificationQueue() {
    return QueueBuilder.durable(EmailVerificationMessagingTopology.QUEUE)
        .deadLetterExchange(MessagingTopology.DEAD_LETTER_EXCHANGE)
        .deadLetterRoutingKey(MessagingTopology.DEAD_LETTER_ROUTING_KEY)
        .build();
  }

  /** Enlaza el contrato versionado con el exchange compartido de trabajos. */
  @Bean
  Binding emailVerificationBinding(Queue emailVerificationQueue, TopicExchange jobsExchange) {
    return BindingBuilder.bind(emailVerificationQueue)
        .to(jobsExchange)
        .with(EmailVerificationMessagingTopology.ROUTING_KEY);
  }
}
