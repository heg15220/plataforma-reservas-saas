package com.reserly.platform.demand.governance;

import java.time.Instant;
import java.util.UUID;

/** Proyección sin evidencia ni identidad del revisor para API interna, admin y local. */
public record DemandHumanReviewResponse(
    UUID id,
    String reviewType,
    String subjectType,
    String subjectKey,
    String subjectVersion,
    UUID venueId,
    String policyVersion,
    String explanationCode,
    String status,
    String reviewReasonCode,
    String correctionVersion,
    String appealCode,
    boolean executionAuthorized,
    Instant submittedAt,
    Instant updatedAt) {}
