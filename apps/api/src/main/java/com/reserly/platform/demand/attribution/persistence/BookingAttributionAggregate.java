package com.reserly.platform.demand.attribution.persistence;

import java.math.BigDecimal;

/** Fila agregada por clase/moneda; nunca expone una reserva o identidad individual. */
public interface BookingAttributionAggregate {

  String getAttributionClass();

  String getCurrency();

  long getReservationsCount();

  long getNewCustomersCount();

  long getOffPeakCoveredCount();

  BigDecimal getAttributedIncome();
}
