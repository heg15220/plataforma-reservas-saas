package com.reserly.platform.billing.dto;

/**
 * Función localizada de un plan.
 *
 * @param code clave estable destinada a renderizado exhaustivo y tests
 * @param label texto visible ya resuelto al idioma de la cuenta
 */
public record PlanFeatureResponse(String code, String label) {}
