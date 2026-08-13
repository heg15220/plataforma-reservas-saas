package com.reserly.platform.demand.attribute.admin;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Proyección segura del catálogo para el panel, sin cargar evidencias ni identidades. */
public record DemandAttributeAdminResponse(
    UUID id,
    String code,
    String family,
    String parentCode,
    String attributeType,
    String nameEs,
    String nameEn,
    List<String> allowedSources,
    String validityMode,
    Integer ttlDays,
    int minimumEvidence,
    String governanceStatus,
    UUID mergedIntoId,
    int version,
    Instant updatedAt) {}
