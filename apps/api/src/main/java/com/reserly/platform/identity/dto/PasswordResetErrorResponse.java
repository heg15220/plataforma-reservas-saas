package com.reserly.platform.identity.dto;

/** Error uniforme que no distingue secreto, cuenta ni política de contraseña. */
public record PasswordResetErrorResponse(String error) {}
