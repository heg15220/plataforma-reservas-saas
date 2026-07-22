package com.reserly.platform.notifications;

/**
 * Error técnico opaco que permite a la futura cola reintentar sin inspeccionar datos personales.
 */
public class EmailDeliveryException extends RuntimeException {

  public EmailDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }
}
