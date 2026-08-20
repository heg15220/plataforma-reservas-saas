package com.reserly.platform.statistics.service;

import com.reserly.platform.demand.attribution.BookingAttributionPolicy;
import com.reserly.platform.demand.attribution.persistence.BookingAttributionAggregate;
import com.reserly.platform.demand.attribution.persistence.BookingAttributionDao;
import com.reserly.platform.statistics.dto.DemandCommercialMetricsResponse;
import com.reserly.platform.statistics.dto.DemandMetricDefinitionResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Construye el agregado comercial sin cargar reservas o evidencia individual en memoria. */
@Component
public class DemandCommercialMetricsAssembler {

  static final int MINIMUM_SAMPLE_SIZE = 10;
  static final String DEFINITIONS_VERSION = "demand-commercial-metrics-v1";
  private static final Set<String> ORIGINATED_CLASSES =
      Set.of("assisted", "generated", "recovered");
  private static final List<DemandMetricDefinitionResponse> DEFINITIONS =
      List.of(
          definition("newCustomers", "NEW_CUSTOMER_FIRST_CONFIRMED_AT_VENUE"),
          definition("originatedReservations", "NON_DIRECT_ATTRIBUTION_CLASSES"),
          definition("offPeakCovered", "WEEKDAY_14_TO_18_LOCAL_NON_DIRECT"),
          definition("attributedIncome", "VISIBLE_PRICE_ASSOCIATED_NOT_INCREMENTAL"),
          definition("coverage", "CLASSIFIED_OVER_CONFIRMED_PERIOD"));

  private final BookingAttributionDao attributionDao;

  public DemandCommercialMetricsAssembler(BookingAttributionDao attributionDao) {
    this.attributionDao = attributionDao;
  }

  /** Agrega el mismo rango de fechas locales de reserva que usa el panel operativo. */
  public DemandCommercialMetricsResponse assemble(
      UUID venueId, LocalDate fromDate, LocalDate toDate, long eligibleReservations, ZoneId zone) {
    List<BookingAttributionAggregate> rows =
        attributionDao.aggregatePeriod(venueId, fromDate, toDate);
    long classified =
        rows.stream().mapToLong(BookingAttributionAggregate::getReservationsCount).sum();
    BigDecimal coverage = percentage(classified, eligibleReservations);
    if (classified < MINIMUM_SAMPLE_SIZE) {
      return response(
          "insufficient_sample",
          zone,
          eligibleReservations,
          classified,
          coverage,
          null,
          null,
          null,
          null,
          null,
          "insufficient_sample",
          null,
          null,
          null,
          null);
    }

    long newCustomers =
        rows.stream().mapToLong(BookingAttributionAggregate::getNewCustomersCount).sum();
    long originated =
        rows.stream()
            .filter(row -> ORIGINATED_CLASSES.contains(row.getAttributionClass()))
            .mapToLong(BookingAttributionAggregate::getReservationsCount)
            .sum();
    long offPeak =
        rows.stream().mapToLong(BookingAttributionAggregate::getOffPeakCoveredCount).sum();
    List<String> currencies =
        rows.stream()
            .filter(row -> row.getAttributedIncome() != null)
            .map(BookingAttributionAggregate::getCurrency)
            .distinct()
            .toList();
    BigDecimal income = null;
    String currency = null;
    String incomeStatus = "no_visible_price";
    if (currencies.size() == 1) {
      String selectedCurrency = currencies.getFirst();
      currency = selectedCurrency;
      income =
          rows.stream()
              .filter(row -> selectedCurrency.equals(row.getCurrency()))
              .map(BookingAttributionAggregate::getAttributedIncome)
              .filter(Objects::nonNull)
              .reduce(BigDecimal.ZERO, BigDecimal::add)
              .setScale(2, RoundingMode.HALF_UP);
      incomeStatus = "available";
    } else if (currencies.size() > 1) {
      incomeStatus = "mixed_currency";
    }
    return response(
        "available",
        zone,
        eligibleReservations,
        classified,
        coverage,
        newCustomers,
        originated,
        offPeak,
        income,
        currency,
        incomeStatus,
        count(rows, "direct"),
        count(rows, "assisted"),
        count(rows, "generated"),
        count(rows, "recovered"));
  }

  private static DemandCommercialMetricsResponse response(
      String status,
      ZoneId zone,
      long eligible,
      long classified,
      BigDecimal coverage,
      Long newCustomers,
      Long originated,
      Long offPeak,
      BigDecimal income,
      String currency,
      String incomeStatus,
      Long direct,
      Long assisted,
      Long generated,
      Long recovered) {
    return new DemandCommercialMetricsResponse(
        status,
        BookingAttributionPolicy.VERSION,
        DEFINITIONS_VERSION,
        zone.getId(),
        MINIMUM_SAMPLE_SIZE,
        eligible,
        classified,
        coverage,
        newCustomers,
        originated,
        offPeak,
        income,
        currency,
        incomeStatus,
        direct,
        assisted,
        generated,
        recovered,
        DEFINITIONS);
  }

  private static long count(List<BookingAttributionAggregate> rows, String attributionClass) {
    return rows.stream()
        .filter(row -> attributionClass.equals(row.getAttributionClass()))
        .mapToLong(BookingAttributionAggregate::getReservationsCount)
        .sum();
  }

  private static BigDecimal percentage(long numerator, long denominator) {
    if (denominator == 0) return BigDecimal.ZERO.setScale(1);
    return BigDecimal.valueOf(numerator)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
  }

  private static DemandMetricDefinitionResponse definition(String key, String code) {
    return new DemandMetricDefinitionResponse(key, code);
  }
}
