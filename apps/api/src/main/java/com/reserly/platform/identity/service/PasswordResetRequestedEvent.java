package com.reserly.platform.identity.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato interno para entregar un enlace de recuperación al titular.
 *
 * <p>{@code token} es sensible y solo puede aparecer en el transporte dirigido al email.
 *
 * @param eventId identificador idempotente de entrega
 * @param userId cuenta destinataria
 * @param email dirección validada y persistida
 * @param preferredLocale locale de la futura plantilla
 * @param token secreto de un solo uso
 * @param expiresAt caducidad absoluta
 */
public record PasswordResetRequestedEvent(
    UUID eventId,
    UUID userId,
    String email,
    String preferredLocale,
    String token,
    Instant expiresAt) {}
