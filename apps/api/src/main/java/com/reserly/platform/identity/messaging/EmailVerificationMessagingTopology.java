package com.reserly.platform.identity.messaging;

/** Nombres versionados del trabajo asíncrono que deberá consumir el proveedor de email. */
public final class EmailVerificationMessagingTopology {

  public static final String QUEUE = "reserly.identity.email-verification.v1";
  public static final String ROUTING_KEY = "identity.email-verification.requested.v1";

  private EmailVerificationMessagingTopology() {}
}
