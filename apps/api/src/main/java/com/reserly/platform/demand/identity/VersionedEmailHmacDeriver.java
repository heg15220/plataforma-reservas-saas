package com.reserly.platform.demand.identity;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** Deriva HMAC-SHA-256 versionado después de normalizar el email sin persistir el texto. */
@Component
class VersionedEmailHmacDeriver {

  private static final String ALGORITHM = "HmacSHA256";
  private final DemandIdentityHmacProperties properties;

  VersionedEmailHmacDeriver(DemandIdentityHmacProperties properties) {
    this.properties = properties;
  }

  VersionedEmailHmac deriveActive(String email) {
    return derive(normalize(email), properties.activeVersion(), properties.activeSecret());
  }

  VersionedEmailHmac derivePrevious(String email) {
    if (!properties.hasPreviousKey()) {
      return null;
    }
    return derive(normalize(email), properties.previousVersion(), properties.previousSecret());
  }

  private VersionedEmailHmac derive(String normalized, String version, String secret) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      return new VersionedEmailHmac(
          version,
          HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8))));
    } catch (java.security.GeneralSecurityException exception) {
      throw new IllegalStateException("HMAC-SHA-256 no disponible", exception);
    }
  }

  /** Normalización deliberadamente conservadora y compartida por todas las versiones de clave. */
  private String normalize(String email) {
    if (email == null) {
      throw new ProgressiveIdentityException("DEMAND_IDENTITY_EMAIL_INVALID");
    }
    String normalized =
        Normalizer.normalize(email.strip(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    int at = normalized.indexOf('@');
    if (normalized.length() < 3
        || normalized.length() > 320
        || at < 1
        || at != normalized.lastIndexOf('@')
        || at == normalized.length() - 1
        || normalized.chars().anyMatch(character -> Character.isWhitespace(character))) {
      throw new ProgressiveIdentityException("DEMAND_IDENTITY_EMAIL_INVALID");
    }
    return normalized;
  }
}
