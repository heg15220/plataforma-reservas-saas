package com.reserly.platform.billing.dto;

/**
 * Límites declarativos conocidos por la pantalla de planes.
 *
 * @param monthlyReservations reservas mensuales o {@code null} sin límite configurado
 * @param teamResources recursos de equipo o {@code null} sin límite configurado
 * @param customFormFields campos custom o {@code null} sin límite configurado
 * @param galleryImages imágenes de galería o {@code null} sin límite configurado
 */
public record PlanLimitsResponse(
    Integer monthlyReservations,
    Integer teamResources,
    Integer customFormFields,
    Integer galleryImages) {}
