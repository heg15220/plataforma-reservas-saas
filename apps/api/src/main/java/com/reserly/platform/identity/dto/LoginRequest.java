package com.reserly.platform.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciales públicas de login.
 *
 * @param email identidad de acceso; se normaliza exclusivamente en backend
 * @param password secreto transitorio, nunca devuelto ni registrado
 */
public record LoginRequest(
    @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 72) String password) {}
