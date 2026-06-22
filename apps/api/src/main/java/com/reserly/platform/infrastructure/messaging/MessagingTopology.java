package com.reserly.platform.infrastructure.messaging;

/**
 * Nombres estables y versionados de la topología AMQP compartida.
 *
 * <p>Los módulos publican trabajos en {@link #JOBS_EXCHANGE} con una routing key propia y declaran
 * sus colas consumidoras en su contexto. Los mensajes rechazados definitivamente se enrutan a
 * {@link #DEAD_LETTER_EXCHANGE}; la cola de aparcamiento permite inspección y recuperación manual
 * sin perder el payload.
 */
public final class MessagingTopology {

  public static final String JOBS_EXCHANGE = "reserly.jobs.v1";
  public static final String DEAD_LETTER_EXCHANGE = "reserly.jobs.dead-letter.v1";
  public static final String DEAD_LETTER_QUEUE = "reserly.jobs.dead-letter.v1";
  public static final String DEAD_LETTER_ROUTING_KEY = "jobs.dead-letter";

  private MessagingTopology() {}
}
