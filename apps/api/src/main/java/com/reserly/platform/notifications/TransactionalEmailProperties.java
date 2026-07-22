package com.reserly.platform.notifications;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración pública no secreta del remitente transaccional.
 *
 * <p>Host, usuario y contraseña pertenecen a {@code spring.mail} y se inyectan exclusivamente desde
 * el entorno. {@code provider} identifica la integración operativa sin acoplar el dominio a Brevo o
 * Mailpit.
 *
 * @param enabled permite desactivar entregas en procesos que solo renderizan plantillas
 * @param provider identificador técnico del proveedor configurado
 * @param fromAddress remitente verificado por el proveedor
 * @param fromName nombre visible del remitente
 */
@Validated
@ConfigurationProperties(prefix = "reserly.notifications.email")
public record TransactionalEmailProperties(
    boolean enabled,
    @NotBlank @Pattern(regexp = "^[a-z][a-z0-9_-]{1,31}$") String provider,
    @NotBlank @Email @Size(max = 320) String fromAddress,
    @NotBlank @Size(max = 100) String fromName) {}
