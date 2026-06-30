package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.ForgotPasswordRequest;
import com.reserly.platform.identity.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Contrato público de solicitud y consumo de recuperación de contraseña. */
@RequestMapping(path = "/api/auth/password", produces = MediaType.APPLICATION_JSON_VALUE)
public interface PasswordResetController {

  /** Acepta la solicitud sin revelar si la cuenta existe o es recuperable. */
  @PostMapping(path = "/forgot", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request);

  /** Reemplaza la credencial mediante un desafío válido de un solo uso. */
  @PostMapping(path = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<Void> reset(@Valid @RequestBody ResetPasswordRequest request);
}
