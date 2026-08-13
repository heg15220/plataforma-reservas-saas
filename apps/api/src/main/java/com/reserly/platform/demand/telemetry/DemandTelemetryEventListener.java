package com.reserly.platform.demand.telemetry;

import com.reserly.platform.demand.ingestion.DemandEventIngestionService;
import com.reserly.platform.demand.ingestion.EventIngestionRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Persiste después de commit y absorbe cualquier fallo para proteger flujos críticos. */
@Component
public class DemandTelemetryEventListener {

  private final DemandEventIngestionService ingestionService;
  private final MeterRegistry meterRegistry;

  public DemandTelemetryEventListener(
      DemandEventIngestionService ingestionService, MeterRegistry meterRegistry) {
    this.ingestionService = ingestionService;
    this.meterRegistry = meterRegistry;
  }

  /** Ejecuta fuera del hilo operativo; nunca registra el evento ni propaga excepciones. */
  @Async("demandTelemetryExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void record(DemandTelemetryEvent event) {
    try {
      ingestionService.ingestTrusted(
          new EventIngestionRequest(
              event.eventId(),
              (short) 1,
              event.eventType(),
              event.occurredAt(),
              event.requestId(),
              "analytics",
              null,
              null,
              null,
              null,
              event.venueId(),
              event.serviceId(),
              event.resourceId(),
              event.timeSlotId(),
              null,
              event.context()));
    } catch (RuntimeException exception) {
      Counter.builder("reserly.demand.telemetry.dropped")
          .tag("eventType", event.eventType())
          .register(meterRegistry)
          .increment();
    }
  }
}
