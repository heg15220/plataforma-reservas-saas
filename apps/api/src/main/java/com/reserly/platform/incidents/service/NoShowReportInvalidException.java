package com.reserly.platform.incidents.service;

/** Solicitud sin confirmación explícita o con identidad persistida inconsistente. */
public class NoShowReportInvalidException extends RuntimeException {

  public NoShowReportInvalidException() {
    super("Invalid no-show report");
  }
}
