package com.reserly.platform.demand.correlation;

import com.reserly.platform.demand.event.persistence.BehaviorEventDao;
import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reconciliación determinista por `requestId`; no infiere causalidad ni resuelve por PII. */
@Service
public class DemandEventReconciliationServiceImpl implements DemandEventReconciliationService {

  private final BehaviorEventDao eventDao;

  public DemandEventReconciliationServiceImpl(BehaviorEventDao eventDao) {
    this.eventDao = eventDao;
  }

  @Override
  @Transactional(readOnly = true)
  public DemandEventReconciliation reconcile(UUID requestId) {
    if (requestId == null) {
      throw new IllegalArgumentException("requestId is required");
    }
    List<BehaviorEventEntity> events = eventDao.findByRequestIdOrdered(requestId);
    List<DemandEventReference> frontend = references(events, "web");
    List<DemandEventReference> backend = references(events, "spring");
    String status = status(frontend, backend);
    return new DemandEventReconciliation(requestId, status, frontend, backend);
  }

  private List<DemandEventReference> references(List<BehaviorEventEntity> events, String producer) {
    return events.stream()
        .filter(event -> producer.equals(event.getProducer()))
        .map(
            event ->
                new DemandEventReference(
                    event.getEventId(),
                    event.getEventType(),
                    event.getProducer(),
                    event.getOccurredAt()))
        .toList();
  }

  private String status(List<DemandEventReference> frontend, List<DemandEventReference> backend) {
    if (!frontend.isEmpty() && !backend.isEmpty()) {
      return "matched";
    }
    if (!frontend.isEmpty()) {
      return "frontend_only";
    }
    if (!backend.isEmpty()) {
      return "backend_only";
    }
    return "not_found";
  }
}
