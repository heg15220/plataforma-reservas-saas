package com.reserly.platform.infrastructure.ratelimit;

/** Error público estable que no revela operación, cuota, clave ni infraestructura interna. */
public record RateLimitErrorResponse(String error) {}
