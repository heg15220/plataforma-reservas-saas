package com.reserly.platform.demand.ingestion;

/** Señala que el interruptor operativo ha cerrado temporalmente la ingesta. */
public class DemandIngestionDisabledException extends RuntimeException {

  public DemandIngestionDisabledException() {
    super("Demand ingestion is disabled");
  }
}
