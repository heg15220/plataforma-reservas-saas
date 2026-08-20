package com.reserly.platform.demand.experiment;

/** Error opaco para entradas inválidas, experimentos inactivos o conflictos de exclusión. */
public class ExperimentAssignmentException extends RuntimeException {
  public ExperimentAssignmentException(String code) {
    super(code);
  }
}
