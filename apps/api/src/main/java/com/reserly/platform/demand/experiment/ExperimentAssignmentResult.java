package com.reserly.platform.demand.experiment;

import java.time.Instant;
import java.util.UUID;

/** Contrato mínimo que el ranking necesita para aplicar una política experimental. */
public record ExperimentAssignmentResult(
    UUID assignmentId,
    String experimentKey,
    int experimentVersion,
    String variantKey,
    String policyVersion,
    int bucket,
    Instant assignedAt,
    Instant exposureRecordedAt) {}
