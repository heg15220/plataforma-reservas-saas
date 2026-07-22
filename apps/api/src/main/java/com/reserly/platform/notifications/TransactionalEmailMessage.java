package com.reserly.platform.notifications;

import java.util.Objects;

/** Mensaje localizado listo para entregar al proveedor. */
public record TransactionalEmailMessage(
    String recipient, String subject, String textBody, String htmlBody) {

  public TransactionalEmailMessage {
    Objects.requireNonNull(recipient);
    Objects.requireNonNull(subject);
    Objects.requireNonNull(textBody);
    Objects.requireNonNull(htmlBody);
  }
}
