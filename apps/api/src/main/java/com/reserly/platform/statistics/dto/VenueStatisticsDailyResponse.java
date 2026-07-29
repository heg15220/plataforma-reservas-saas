package com.reserly.platform.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Punto diario minimizado para gráficos y alternativas textuales del panel privado. */
public record VenueStatisticsDailyResponse(
    LocalDate date,
    long reservationsCount,
    long confirmedCount,
    long cancelledCount,
    long noShowCount,
    long attendedCount,
    long occupiedCapacity,
    long availableCapacity,
    BigDecimal occupancyRate,
    long reviewsCount,
    BigDecimal averageRating) {}
