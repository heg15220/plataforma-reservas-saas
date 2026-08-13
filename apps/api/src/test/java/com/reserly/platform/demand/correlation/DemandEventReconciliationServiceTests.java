package com.reserly.platform.demand.correlation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.event.persistence.BehaviorEventDao;
import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Comprueba reconciliación por productor sin contexto ni identidad. */
class DemandEventReconciliationServiceTests {

  @Test
  void classifiesMatchedFrontendBackendTraceAndReturnsOnlySafeReferences() {
    BehaviorEventDao dao = mock(BehaviorEventDao.class);
    UUID requestId = UUID.randomUUID();
    when(dao.findByRequestIdOrdered(requestId))
        .thenReturn(
            List.of(
                event("searchPerformed", "web", Instant.parse("2026-08-13T10:00:00Z")),
                event("bookingStarted", "spring", Instant.parse("2026-08-13T10:05:00Z")),
                event("bookingCompleted", "spring", Instant.parse("2026-08-13T10:06:00Z"))));

    DemandEventReconciliation result =
        new DemandEventReconciliationServiceImpl(dao).reconcile(requestId);

    assertThat(result.status()).isEqualTo("matched");
    assertThat(result.frontendEvents())
        .extracting(DemandEventReference::eventType)
        .containsExactly("searchPerformed");
    assertThat(result.backendEvents())
        .extracting(DemandEventReference::eventType)
        .containsExactly("bookingStarted", "bookingCompleted");
    assertThat(result.toString()).doesNotContain("contextJson", "customerId", "sessionId");
  }

  @Test
  void distinguishesPartialAndMissingTracesAndRejectsNull() {
    BehaviorEventDao dao = mock(BehaviorEventDao.class);
    DemandEventReconciliationService service = new DemandEventReconciliationServiceImpl(dao);
    UUID frontendId = UUID.randomUUID();
    UUID backendId = UUID.randomUUID();
    UUID missingId = UUID.randomUUID();
    when(dao.findByRequestIdOrdered(frontendId))
        .thenReturn(List.of(event("bookingAbandoned", "web", Instant.now())));
    when(dao.findByRequestIdOrdered(backendId))
        .thenReturn(List.of(event("bookingCompleted", "spring", Instant.now())));
    when(dao.findByRequestIdOrdered(missingId)).thenReturn(List.of());

    assertThat(service.reconcile(frontendId).status()).isEqualTo("frontend_only");
    assertThat(service.reconcile(backendId).status()).isEqualTo("backend_only");
    assertThat(service.reconcile(missingId).status()).isEqualTo("not_found");
    assertThatThrownBy(() -> service.reconcile(null)).isInstanceOf(IllegalArgumentException.class);
  }

  private BehaviorEventEntity event(String type, String producer, Instant occurredAt) {
    BehaviorEventEntity event = new BehaviorEventEntity();
    event.setEventId(UUID.randomUUID());
    event.setEventType(type);
    event.setProducer(producer);
    event.setOccurredAt(occurredAt);
    event.setContextJson(java.util.Map.of("hidden", "not projected"));
    return event;
  }
}
