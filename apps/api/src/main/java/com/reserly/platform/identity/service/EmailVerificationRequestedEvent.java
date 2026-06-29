package com.reserly.platform.identity.service;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato interno para entregar el enlace mediante la infraestructura transaccional de email.
 *
 * <p>{@code token} es sensible: ningún listener puede registrarlo ni persistirlo en claro fuera del
 * transporte de entrega.
 *
 * @param eventId identificador idempotente de la solicitud
 * @param userId destinatario propietario
 * @param email dirección original validada
 * @param preferredLocale locale de plantilla
 * @param token secreto de un solo uso
 * @param expiresAt caducidad absoluta
 */
public record EmailVerificationRequestedEvent(
    UUID eventId,
    UUID userId,
    String email,
    String preferredLocale,
    String token,
    Instant expiresAt) {}
