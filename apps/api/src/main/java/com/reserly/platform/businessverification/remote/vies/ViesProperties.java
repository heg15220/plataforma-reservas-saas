package com.reserly.platform.businessverification.remote.vies;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración pública del servicio oficial VIES.
 *
 * @param endpoint endpoint SOAP oficial
 * @param maxResponseBytes límite defensivo del cuerpo XML
 */
@Validated
@ConfigurationProperties(prefix = "reserly.business-verification.vies")
public record ViesProperties(@NotNull URI endpoint, @Min(1024) @Max(1048576) int maxResponseBytes) {

  @AssertTrue(message = "El endpoint VIES debe usar HTTPS")
  public boolean isHttpsEndpoint() {
    return endpoint != null && "https".equalsIgnoreCase(endpoint.getScheme());
  }
}
