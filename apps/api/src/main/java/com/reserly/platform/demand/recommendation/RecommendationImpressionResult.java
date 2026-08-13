package com.reserly.platform.demand.recommendation;

import java.util.List;
import java.util.UUID;

/** Resultado minimizado de una impresión validada. */
public record RecommendationImpressionResult(
    UUID impressionId, UUID recommendationRequestId, List<UUID> visibleCandidateIds) {}
