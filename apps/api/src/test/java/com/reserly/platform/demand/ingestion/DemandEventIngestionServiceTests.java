package com.reserly.platform.demand.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.event.persistence.BehaviorEventDao;
import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import com.reserly.platform.demand.identity.persistence.AnonymousIdentityDao;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityDao;
import com.reserly.platform.infrastructure.ratelimit.RateLimitService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

/** Verifica validación previa, idempotencia, cuota y ausencia de persistencia parcial. */
class DemandEventIngestionServiceTests {

  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");
  private BehaviorEventDao eventDao;
  private RateLimitService rateLimitService;
  private SimpleMeterRegistry meterRegistry;
  private DemandEventIngestionServiceImpl service;

  @BeforeEach
  void setUp() {
    eventDao = mock(BehaviorEventDao.class);
    rateLimitService = mock(RateLimitService.class);
    meterRegistry = new SimpleMeterRegistry();
    service =
        new DemandEventIngestionServiceImpl(
            eventDao,
            mock(AnonymousIdentityDao.class),
            mock(CustomerIdentityDao.class),
            rateLimitService,
            new DemandIngestionProperties(
                true,
                "test-producer",
                "test-demand-token-at-least-32-characters",
                100,
                Duration.ofDays(90)),
            new ObjectMapper(),
            meterRegistry,
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void acceptsValidEventAndReturnsExistingEventAsDuplicate() {
    EventIngestionRequest accepted = event(UUID.randomUUID(), Map.of("queryLength", 12));
    EventIngestionRequest duplicate = event(UUID.randomUUID(), Map.of());
    when(eventDao.findByEventId(accepted.eventId())).thenReturn(Optional.empty());
    when(eventDao.findByEventId(duplicate.eventId()))
        .thenReturn(Optional.of(new BehaviorEventEntity()));

    EventBatchIngestionResponse response =
        service.ingest(
            "test-producer", new EventBatchIngestionRequest(List.of(accepted, duplicate)));

    assertThat(response.acceptedCount()).isEqualTo(1);
    assertThat(response.duplicateCount()).isEqualTo(1);
    assertThat(response.results())
        .extracting(EventIngestionItemResponse::status)
        .containsExactly("accepted", "duplicate");
    verify(rateLimitService).check(any(), any());
    verify(eventDao).saveAndFlush(any(BehaviorEventEntity.class));
    assertThat(
            meterRegistry
                .get("reserly.demand.events.outcomes")
                .tags("eventType", "searchPerformed", "schemaVersion", "1", "result", "accepted")
                .counter()
                .count())
        .isOne();
    assertThat(
            meterRegistry
                .find("reserly.demand.events.latency")
                .tags("eventType", "searchPerformed", "schemaVersion", "1")
                .timers())
        .isNotEmpty();
  }

  @Test
  void rejectsWholeBatchBeforePersistenceWhenContextContainsPii() {
    EventIngestionRequest valid = event(UUID.randomUUID(), Map.of("queryLength", 2));
    EventIngestionRequest invalid =
        event(UUID.randomUUID(), Map.of("email", "forbidden@example.invalid"));

    assertThatThrownBy(
            () ->
                service.ingest(
                    "test-producer", new EventBatchIngestionRequest(List.of(valid, invalid))))
        .isInstanceOf(DemandIngestionException.class)
        .extracting("code")
        .isEqualTo("CONTEXT_INVALID");
    verify(eventDao, never()).saveAndFlush(any());
    assertThat(
            meterRegistry
                .get("reserly.demand.events.rejections")
                .tags(
                    "eventType", "searchPerformed", "schemaVersion", "1", "code", "CONTEXT_INVALID")
                .counter()
                .count())
        .isOne();
  }

  @Test
  void rejectsDuplicateIdsWithinBatchAndIdentifiersOutsideCatalog() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(
            () ->
                service.ingest(
                    "test-producer",
                    new EventBatchIngestionRequest(
                        List.of(event(id, Map.of()), event(id, Map.of())))))
        .isInstanceOf(DemandIngestionException.class)
        .extracting("code")
        .isEqualTo("BATCH_DUPLICATE_ID");

    EventIngestionRequest forbiddenVenue =
        new EventIngestionRequest(
            UUID.randomUUID(),
            (short) 1,
            "searchPerformed",
            NOW.minusSeconds(1),
            UUID.randomUUID(),
            "analytics",
            null,
            UUID.randomUUID(),
            null,
            null,
            UUID.randomUUID(),
            null,
            null,
            null,
            "ES",
            Map.of());
    assertThatThrownBy(
            () ->
                service.ingest(
                    "test-producer", new EventBatchIngestionRequest(List.of(forbiddenVenue))))
        .isInstanceOf(DemandIngestionException.class)
        .extracting("code")
        .isEqualTo("IDENTIFIER_NOT_ALLOWED");
  }

  private EventIngestionRequest event(UUID eventId, Map<String, Object> context) {
    return new EventIngestionRequest(
        eventId,
        (short) 1,
        "searchPerformed",
        NOW.minusSeconds(1),
        UUID.randomUUID(),
        "analytics",
        null,
        UUID.randomUUID(),
        null,
        null,
        null,
        null,
        null,
        null,
        "ES",
        context);
  }
}
