package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.converter.AuthenticationConverter;
import com.reserly.platform.identity.dto.LoginRequest;
import com.reserly.platform.identity.dto.LoginResponse;
import com.reserly.platform.identity.service.AuthenticationService;
import com.reserly.platform.identity.service.LoginOutcome;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP que mantiene el secreto fuera del cuerpo JSON. */
@RestController
public class AuthenticationControllerImpl implements AuthenticationController {

  private final AuthenticationService authenticationService;
  private final AuthenticationConverter converter;
  private final SessionCookieFactory cookieFactory;

  public AuthenticationControllerImpl(
      AuthenticationService authenticationService,
      AuthenticationConverter converter,
      SessionCookieFactory cookieFactory) {
    this.authenticationService = authenticationService;
    this.converter = converter;
    this.cookieFactory = cookieFactory;
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest request) {
    LoginOutcome outcome = authenticationService.login(converter.toCommand(request));
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookieFactory.create(outcome.sessionToken()).toString())
        .body(converter.toResponse(outcome));
  }

  @Override
  public ResponseEntity<Void> logout(String sessionToken) {
    authenticationService.logout(sessionToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
        .build();
  }
}
