package com.reserly.platform.identity.messaging;

/** Nombres versionados del trabajo asíncrono de recuperación de contraseña. */
public final class PasswordResetMessagingTopology {

  public static final String QUEUE = "reserly.identity.password-reset.v1";
  public static final String ROUTING_KEY = "identity.password-reset.requested.v1";

  private PasswordResetMessagingTopology() {}
}
