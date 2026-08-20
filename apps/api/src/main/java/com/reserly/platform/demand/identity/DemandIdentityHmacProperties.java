package com.reserly.platform.demand.identity;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Material HMAC inyectado por entorno y plazos de la vinculación progresiva.
 *
 * <p>Solo se admite una clave anterior durante una rotación controlada. Retirarla después de migrar
 * evita conservar secretos antiguos indefinidamente.
 */
@ConfigurationProperties("reserly.demand.identity.hmac")
public record DemandIdentityHmacProperties(
    String activeVersion,
    String activeSecret,
    String previousVersion,
    String previousSecret,
    Duration customerRetention,
    Duration linkRetention) {

  public DemandIdentityHmacProperties {
    requireVersion(activeVersion, "La versión HMAC activa");
    requireSecret(activeSecret, "La clave HMAC activa");
    boolean hasPreviousVersion = previousVersion != null && !previousVersion.isBlank();
    boolean hasPreviousSecret = previousSecret != null && !previousSecret.isBlank();
    if (hasPreviousVersion != hasPreviousSecret) {
      throw new IllegalArgumentException("La clave HMAC anterior requiere versión y secreto");
    }
    if (hasPreviousVersion) {
      requireVersion(previousVersion, "La versión HMAC anterior");
      requireSecret(previousSecret, "La clave HMAC anterior");
      if (activeVersion.equals(previousVersion)) {
        throw new IllegalArgumentException("Las versiones HMAC activa y anterior deben diferir");
      }
    }
    if (customerRetention == null
        || customerRetention.isNegative()
        || customerRetention.isZero()
        || linkRetention == null
        || linkRetention.isNegative()
        || linkRetention.isZero()) {
      throw new IllegalArgumentException("Las retenciones de identidad deben ser positivas");
    }
  }

  public boolean hasPreviousKey() {
    return previousVersion != null && !previousVersion.isBlank();
  }

  private static void requireVersion(String value, String label) {
    if (value == null || !value.matches("^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$")) {
      throw new IllegalArgumentException(label + " no es válida");
    }
  }

  private static void requireSecret(String value, String label) {
    if (value == null || value.length() < 32) {
      throw new IllegalArgumentException(label + " debe tener al menos 32 caracteres");
    }
  }
}
