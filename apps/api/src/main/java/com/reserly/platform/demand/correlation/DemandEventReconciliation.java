package com.reserly.platform.demand.correlation;

import java.util.List;
import java.util.UUID;

/** Vista segura de cobertura entre observaciones web y resultados canónicos Spring. */
public record DemandEventReconciliation(
    UUID requestId,
    String status,
    List<DemandEventReference> frontendEvents,
    List<DemandEventReference> backendEvents) {}
