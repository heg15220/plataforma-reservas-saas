package com.reserly.platform.administration.dto;

import java.time.Instant;

/** Snapshot global agregado sin identidades de clientes ni datos fiscales. */
public record AdminMetricsResponse(
    long totalVenues,
    long publishedVenues,
    long suspendedVenues,
    long totalReservations,
    long confirmedReservations,
    long totalBusinessAccounts,
    long pendingBusinessReviews,
    long activeSubscriptions,
    long activePenalties,
    Instant generatedAt) {}
