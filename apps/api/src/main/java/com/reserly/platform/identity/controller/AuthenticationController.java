package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.LoginRequest;
import com.reserly.platform.identity.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Contrato público de login y logout de locales.
 *
 * <p>Login devuelve metadatos no sensibles y establece cookie. Logout es idempotente y siempre
 * elimina la cookie recibida.
 */
@RequestMapping(path = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public interface AuthenticationController {

  /** Autentica una cuenta empresarial operativa y crea una sesión revocable. */
  @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request);

  /** Revoca la sesión si existe; no revela validez de la cookie. */
  @PostMapping(path = "/logout")
  ResponseEntity<Void> logout(
      @CookieValue(name = SessionCookieFactory.COOKIE_NAME, required = false) String sessionToken);
}
