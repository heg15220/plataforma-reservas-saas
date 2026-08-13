package com.reserly.platform.demand.attribute.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proyección de revisión con ejemplos ya resumidos y sin contenido original. */
public record DemandAttributeCandidateAdminResponse(
    UUID id,
    String proposedCode,
    String clusterKey,
    String family,
    String attributeType,
    String nameEs,
    String nameEn,
    List<String> allowedSources,
    List<String> exampleSummaries,
    String governanceStatus,
    String decisionReason,
    UUID resultingAttributeId,
    int version,
    Instant updatedAt) {}
