package com.reserly.platform.identity.controller;

import com.reserly.platform.configuration.ReserlyProperties;
import com.reserly.platform.identity.service.SessionProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Construye la única cookie de sesión con atributos seguros consistentes.
 *
 * <p>No define dominio para mantenerla host-only. CSRF se endurecerá en su tarea específica.
 */
@Component
public class SessionCookieFactory {

  public static final String COOKIE_NAME = "reserly_session";

  private final ReserlyProperties reserlyProperties;
  private final SessionProperties sessionProperties;

  public SessionCookieFactory(
      ReserlyProperties reserlyProperties, SessionProperties sessionProperties) {
    this.reserlyProperties = reserlyProperties;
    this.sessionProperties = sessionProperties;
  }

  /** Cookie persistente de sesión, inaccesible para JavaScript. */
  public ResponseCookie create(String token) {
    return base(token).maxAge(sessionProperties.lifetime()).build();
  }

  /** Tombstone host-only que elimina la cookie en logout aunque la sesión sea desconocida. */
  public ResponseCookie clear() {
    return base("").maxAge(Duration.ZERO).build();
  }

  private ResponseCookie.ResponseCookieBuilder base(String value) {
    return ResponseCookie.from(COOKIE_NAME, value)
        .httpOnly(true)
        .secure(reserlyProperties.security().secureCookies())
        .sameSite("Strict")
        .path("/");
  }
}
