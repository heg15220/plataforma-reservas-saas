package com.reserly.platform.identity.dto;

import java.util.Objects;

/** Entrada interna de login; su contraseña solo vive durante la invocación. */
public record LoginCommand(String email, String rawPassword) {

  public LoginCommand {
    Objects.requireNonNull(email);
    Objects.requireNonNull(rawPassword);
  }
}
