/**
 * Protección distribuida de endpoints y casos de uso sensibles.
 *
 * <p>Las cuotas viven en Redis con TTL, usan discriminadores hasheados y son auxiliares: no
 * sustituyen autenticación, autorización, idempotencia ni auditoría.
 */
package com.reserly.platform.infrastructure.ratelimit;
