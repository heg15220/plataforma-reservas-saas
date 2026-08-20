package com.reserly.platform.demand.identity;

/** Error opaco de consentimiento, identidad o conflicto; no incluye email ni digest. */
public class ProgressiveIdentityException extends RuntimeException {
  public ProgressiveIdentityException(String code) {
    super(code);
  }
}
