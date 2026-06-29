package com.reserly.platform.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Solicitud de consumo de un desafío de verificación.
 *
 * @param token secreto Base64 URL-safe recibido en el enlace
 */
public record VerifyEmailRequest(@NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String token) {}
