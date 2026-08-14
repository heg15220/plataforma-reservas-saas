package com.reserly.platform.demand.candidate;

import java.util.UUID;

/** Candidato elegible con señales de recuperación explicables y sin datos personales. */
public record HybridCandidate(
    UUID venueId,
    UUID serviceId,
    String categoryCode,
    int distanceMeters,
    int availableSlotCount,
    double fullTextScore,
    double trigramScore,
    double vectorScore,
    double retrievalScore,
    String retrievalPolicyVersion) {}
