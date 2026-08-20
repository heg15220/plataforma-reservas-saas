package com.reserly.platform.demand.attribution;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Ejecuta la proyección después del commit y absorbe fallos para proteger la reserva. */
@Component
public class BookingAttributionEventListener {

  private final BookingAttributionService attributionService;
  private final MeterRegistry meterRegistry;

  public BookingAttributionEventListener(
      BookingAttributionService attributionService, MeterRegistry meterRegistry) {
    this.attributionService = attributionService;
    this.meterRegistry = meterRegistry;
  }

  /** La cola acotada comparte la semántica descartable de telemetría del motor. */
  @Async("demandTelemetryExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void attribute(BookingAttributionRequestedEvent event) {
    try {
      attributionService.attribute(event.reservationId(), event.requestId(), event.confirmedAt());
      Counter.builder("reserly.demand.attribution.completed").register(meterRegistry).increment();
    } catch (RuntimeException exception) {
      Counter.builder("reserly.demand.attribution.failed").register(meterRegistry).increment();
    }
  }
}
