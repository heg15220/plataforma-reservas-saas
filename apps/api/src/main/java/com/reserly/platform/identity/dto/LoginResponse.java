package com.reserly.platform.identity.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadatos no sensibles de una sesión creada.
 *
 * <p>El secreto se entrega únicamente como cookie HttpOnly.
 */
public record LoginResponse(
    UUID userId,
    String accountType,
    String preferredLocale,
    boolean emailVerified,
    Instant sessionExpiresAt) {}
