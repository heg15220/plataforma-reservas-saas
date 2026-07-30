package com.reserly.platform.administration.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Límites conocidos; {@code null} expresa ausencia de límite configurado. */
public record AdminPlanLimits(
    @PositiveOrZero Integer monthlyReservations,
    @PositiveOrZero Integer teamResources,
    @PositiveOrZero Integer customFormFields,
    @PositiveOrZero Integer galleryImages) {}
