package com.reserly.platform.demand.identity;

import java.time.Instant;
import java.util.UUID;

/**
 * Evidencia para vincular sesión, identidad anónima y cliente al confirmar un email.
 *
 * @param sessionId sesión efímera generada aleatoriamente
 * @param anonymousIdentityId identidad de primera parte previamente consentida
 * @param email email operativo usado solo en memoria para HMAC
 * @param purpose finalidad única autorizada
 * @param consentVersion documento aceptado
 * @param consentedAt instante de aceptación
 * @param linkReason motivo cerrado y verificable
 */
public record ProgressiveIdentityCommand(
    UUID sessionId,
    UUID anonymousIdentityId,
    String email,
    String purpose,
    String consentVersion,
    Instant consentedAt,
    String linkReason) {}
