package com.reserly.platform.businessverification.document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Límites del protocolo INSTREAM de ClamAV.
 *
 * @param host host privado de clamd
 * @param port puerto TCP
 * @param connectTimeout timeout de conexión
 * @param readTimeout timeout de respuesta
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.documents.antivirus")
public record ClamAvProperties(
    @NotBlank String host,
    @Min(1) @Max(65535) int port,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout) {}
