package com.reserly.platform.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara la topología AMQP mínima compartida por los trabajos asíncronos.
 *
 * <p>La configuración solo crea exchanges y la cola de aparcamiento. Cada módulo de negocio debe
 * declarar una cola durable propia, enlazarla con una routing key específica y configurar su dead
 * lettering hacia {@link MessagingTopology#DEAD_LETTER_EXCHANGE}. Este límite evita una cola
 * genérica con consumidores incompatibles.
 */
@Configuration(proxyBeanMethods = false)
public class MessagingConfiguration {

  /**
   * Exchange principal para publicar trabajos de dominio sin acoplar productores y consumidores.
   *
   * @return exchange topic durable, no autoeliminable
   */
  @Bean
  TopicExchange jobsExchange() {
    return new TopicExchange(MessagingTopology.JOBS_EXCHANGE, true, false);
  }

  /**
   * Exchange de destino para mensajes agotados o rechazados definitivamente.
   *
   * @return exchange topic durable, no autoeliminable
   */
  @Bean
  TopicExchange deadLetterExchange() {
    return new TopicExchange(MessagingTopology.DEAD_LETTER_EXCHANGE, true, false);
  }

  /**
   * Cola durable de aparcamiento. No tiene consumidor automático para evitar ciclos de reintento.
   *
   * @return cola durable compartida para inspección operativa
   */
  @Bean
  Queue deadLetterQueue() {
    return new Queue(MessagingTopology.DEAD_LETTER_QUEUE, true, false, false);
  }

  /**
   * Enlaza los mensajes no procesables con la cola de aparcamiento.
   *
   * @param deadLetterQueue cola durable de destino
   * @param deadLetterExchange exchange de mensajes no procesables
   * @return binding estable con routing key versionada
   */
  @Bean
  Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
    return BindingBuilder.bind(deadLetterQueue)
        .to(deadLetterExchange)
        .with(MessagingTopology.DEAD_LETTER_ROUTING_KEY);
  }
}
