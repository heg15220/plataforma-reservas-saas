package com.reserly.platform.businessverification.service;

/** Error interno sin datos fiscales para una cuenta empresarial inexistente. */
public class BusinessAccountNotFoundException extends RuntimeException {

  public BusinessAccountNotFoundException() {
    super("Business account does not exist");
  }
}
