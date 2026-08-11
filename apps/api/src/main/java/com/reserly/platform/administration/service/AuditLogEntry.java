package com.reserly.platform.administration.service;

import java.util.Map;
import java.util.UUID;

/**
 * Comando interno para registrar una acción crítica.
 *
 * @param actorUserId usuario autenticado, o nulo para un proceso interno
 * @param actorRole rol efectivo {@code venue_owner}, {@code admin} o {@code system}
 * @param entityType tipo estable de agregado afectado
 * @param entityId identificador del agregado afectado
 * @param action acción estable y consultable
 * @param beforeJson snapshot previo minimizado
 * @param afterJson snapshot posterior minimizado
 * @param ipAddress dirección observada directamente por el servidor
 * @param userAgent cabecera acotada, tratada como dato no confiable
 */
public record AuditLogEntry(
    UUID actorUserId,
    String actorRole,
    String entityType,
    UUID entityId,
    String action,
    Map<String, Object> beforeJson,
    Map<String, Object> afterJson,
    String ipAddress,
    String userAgent) {}
