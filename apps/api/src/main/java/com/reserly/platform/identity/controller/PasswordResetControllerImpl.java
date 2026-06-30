package com.reserly.platform.identity.controller;

import com.reserly.platform.identity.dto.ForgotPasswordRequest;
import com.reserly.platform.identity.dto.ResetPasswordRequest;
import com.reserly.platform.identity.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Adaptador HTTP de recuperación de contraseña. */
@RestController
public class PasswordResetControllerImpl implements PasswordResetController {

  private final PasswordResetService passwordResetService;

  public PasswordResetControllerImpl(PasswordResetService passwordResetService) {
    this.passwordResetService = passwordResetService;
  }

  @Override
  public ResponseEntity<Void> forgot(ForgotPasswordRequest request) {
    passwordResetService.requestReset(request.email());
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<Void> reset(ResetPasswordRequest request) {
    passwordResetService.resetPassword(request.token(), request.newPassword());
    return ResponseEntity.noContent().build();
  }
}
