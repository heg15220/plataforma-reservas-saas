package com.reserly.platform.demand.retention;

/** Contadores técnicos del lote; no incluyen IDs ni contenido eliminado. */
public record DemandRetentionResult(
    int events,
    int recommendations,
    int profiles,
    int evidences,
    int links,
    int anonymousIdentities,
    int customerIdentities,
    int privacyAudits) {}
