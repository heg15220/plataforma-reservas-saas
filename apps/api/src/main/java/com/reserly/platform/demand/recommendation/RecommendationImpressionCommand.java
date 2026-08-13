package com.reserly.platform.demand.recommendation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Describe una superficie que el consumidor confirma haber renderizado.
 *
 * @param impressionId identificador idempotente de la impresión
 * @param recommendationRequestId identificador público de la decisión auditada
 * @param candidateIds candidatos realmente visibles, en el orden observado
 * @param occurredAt instante UTC de la exposición
 */
public record RecommendationImpressionCommand(
    UUID impressionId, UUID recommendationRequestId, List<UUID> candidateIds, Instant occurredAt) {}
