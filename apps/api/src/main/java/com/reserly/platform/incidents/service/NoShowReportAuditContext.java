package com.reserly.platform.incidents.service;

/**
 * Metadatos técnicos observados por el servidor para auditoría.
 *
 * @param ipAddress dirección remota directa; no se confía en cabeceras de proxy aportadas por el
 *     cliente
 * @param userAgent cabecera no confiable que será recortada antes de persistirse
 */
public record NoShowReportAuditContext(String ipAddress, String userAgent) {}
