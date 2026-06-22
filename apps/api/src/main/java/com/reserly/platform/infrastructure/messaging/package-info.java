/**
 * Infraestructura AMQP compartida para trabajos asíncronos y eventos internos no transaccionales.
 *
 * <p>Publicar en RabbitMQ no sustituye la transacción de PostgreSQL. Los futuros productores que
 * necesiten entrega atómica con cambios de dominio deberán implementar un outbox persistente.
 */
package com.reserly.platform.infrastructure.messaging;
