package com.reserly.platform.demand.ingestion;

/** Rechazo esperado identificado exclusivamente por un código de baja cardinalidad. */
public class DemandIngestionException extends RuntimeException {

  private final String code;

  public DemandIngestionException(String code) {
    super(code);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
