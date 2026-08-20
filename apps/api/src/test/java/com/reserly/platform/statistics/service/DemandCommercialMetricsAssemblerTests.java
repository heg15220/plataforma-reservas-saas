package com.reserly.platform.statistics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.attribution.persistence.BookingAttributionAggregate;
import com.reserly.platform.demand.attribution.persistence.BookingAttributionDao;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica umbral, cobertura, desglose y semántica monetaria del panel comercial. */
class DemandCommercialMetricsAssemblerTests {

  private static final UUID VENUE_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
  private static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

  @Test
  void publishesCompleteMetricsAndVisibleDefinitionsAtMinimumSample() {
    BookingAttributionDao dao = mock(BookingAttributionDao.class);
    List<BookingAttributionAggregate> rows =
        List.of(
            row("direct", null, 3, 1, 0, null),
            row("assisted", "EUR", 2, 1, 1, "50.00"),
            row("generated", "EUR", 4, 2, 1, "120.00"),
            row("recovered", "EUR", 1, 0, 1, "75.00"));
    when(dao.aggregatePeriod(VENUE_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30)))
        .thenReturn(rows);
    var assembler = new DemandCommercialMetricsAssembler(dao);

    var result =
        assembler.assemble(VENUE_ID, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30), 12, ZONE);

    assertThat(result.status()).isEqualTo("available");
    assertThat(result.classifiedReservations()).isEqualTo(10);
    assertThat(result.coveragePercent()).isEqualByComparingTo("83.3");
    assertThat(result.newCustomers()).isEqualTo(4);
    assertThat(result.originatedReservations()).isEqualTo(7);
    assertThat(result.offPeakCovered()).isEqualTo(3);
    assertThat(result.attributedIncome()).isEqualByComparingTo("245.00");
    assertThat(result.attributedCurrency()).isEqualTo("EUR");
    assertThat(result.directReservations()).isEqualTo(3);
    assertThat(result.definitions())
        .extracting("key")
        .containsExactly(
            "newCustomers",
            "originatedReservations",
            "offPeakCovered",
            "attributedIncome",
            "coverage");
  }

  @Test
  void suppressesCommercialFiguresBelowMinimumWithoutHidingCoverage() {
    BookingAttributionDao dao = mock(BookingAttributionDao.class);
    List<BookingAttributionAggregate> rows = List.of(row("generated", "EUR", 9, 4, 3, "180.00"));
    when(dao.aggregatePeriod(VENUE_ID, LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29)))
        .thenReturn(rows);
    var assembler = new DemandCommercialMetricsAssembler(dao);

    var result =
        assembler.assemble(
            VENUE_ID, LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29), 10, ZONE);

    assertThat(result.status()).isEqualTo("insufficient_sample");
    assertThat(result.classifiedReservations()).isEqualTo(9);
    assertThat(result.eligibleReservations()).isEqualTo(10);
    assertThat(result.coveragePercent()).isEqualByComparingTo("90.0");
    assertThat(result.newCustomers()).isNull();
    assertThat(result.originatedReservations()).isNull();
    assertThat(result.attributedIncome()).isNull();
    assertThat(result.generatedReservations()).isNull();
    verify(dao).aggregatePeriod(VENUE_ID, LocalDate.of(2026, 7, 29), LocalDate.of(2026, 7, 29));
  }

  private BookingAttributionAggregate row(
      String attributionClass,
      String currency,
      long reservations,
      long newCustomers,
      long offPeak,
      String income) {
    BookingAttributionAggregate row = mock(BookingAttributionAggregate.class);
    when(row.getAttributionClass()).thenReturn(attributionClass);
    when(row.getCurrency()).thenReturn(currency);
    when(row.getReservationsCount()).thenReturn(reservations);
    when(row.getNewCustomersCount()).thenReturn(newCustomers);
    when(row.getOffPeakCoveredCount()).thenReturn(offPeak);
    when(row.getAttributedIncome()).thenReturn(income == null ? null : new BigDecimal(income));
    return row;
  }
}
