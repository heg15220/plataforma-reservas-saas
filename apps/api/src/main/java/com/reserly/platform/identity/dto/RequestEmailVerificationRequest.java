package com.reserly.platform.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud pública de un nuevo desafío.
 *
 * @param email dirección cuyo estado nunca se revela en la respuesta
 */
public record RequestEmailVerificationRequest(@NotBlank @Email @Size(max = 320) String email) {}
