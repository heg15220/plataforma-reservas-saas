package com.reserly.platform.identity.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Coste adaptativo BCrypt.
 *
 * @param bcryptStrength log2 del trabajo; 12 es el baseline y 16 limita errores operativos
 */
@Validated
@ConfigurationProperties(prefix = "reserly.identity.password")
public record PasswordHashingProperties(@Min(12) @Max(16) int bcryptStrength) {}
