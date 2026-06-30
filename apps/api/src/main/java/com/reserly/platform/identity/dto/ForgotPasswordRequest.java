package com.reserly.platform.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Solicitud pública cuyo email nunca se confirma en la respuesta. */
public record ForgotPasswordRequest(@NotBlank @Email @Size(max = 320) String email) {}
