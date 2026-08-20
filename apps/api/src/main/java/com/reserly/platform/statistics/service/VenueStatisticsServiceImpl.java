package com.reserly.platform.statistics.service;

import com.reserly.platform.statistics.dto.VenueStatisticsDailyResponse;
import com.reserly.platform.statistics.dto.VenueStatisticsResponse;
import com.reserly.platform.statistics.persistence.StatsDailyVenueDao;
import com.reserly.platform.statistics.persistence.StatsDailyVenueEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recalcula un rango acotado del local antes de construir totales y evolución.
 *
 * <p>La escritura y lectura comparten transacción. Un {@code venueId} explícito se resuelve con la
 * misma consulta de acceso que el panel multi-local, sin revelar locales ajenos.
 */
@Service
public class VenueStatisticsServiceImpl implements VenueStatisticsService {

  static final int MAX_RANGE_DAYS = 366;

  private final VenueDao venueDao;
  private final StatsDailyVenueDao statsDao;
  private final DemandCommercialMetricsAssembler demandMetricsAssembler;
  private final Clock clock;

  public VenueStatisticsServiceImpl(
      VenueDao venueDao,
      StatsDailyVenueDao statsDao,
      DemandCommercialMetricsAssembler demandMetricsAssembler,
      Clock clock) {
    this.venueDao = venueDao;
    this.statsDao = statsDao;
    this.demandMetricsAssembler = demandMetricsAssembler;
    this.clock = clock;
  }

  @Override
  @Transactional
  public VenueStatisticsResponse findOwned(
      UUID userId, UUID selectedVenueId, String periodValue, LocalDate fromDate, LocalDate toDate) {
    if (userId == null) {
      throw new VenueStatisticsFilterInvalidException();
    }
    VenueStatisticsPeriod period = VenueStatisticsPeriod.parse(periodValue);
    DateRange range = resolveRange(period, fromDate, toDate);
    UUID venueId = resolveAccessibleVenueId(userId, selectedVenueId);

    statsDao.aggregateVenueRange(
        venueId, range.fromDate(), range.toDate(), clock.getZone().getId(), clock.instant());
    List<StatsDailyVenueEntity> days =
        statsDao.findRange(venueId, range.fromDate(), range.toDate());
    return toResponse(venueId, period, range, days);
  }

  /** Resuelve el local solicitado sin distinguir entre un UUID inexistente, archivado o ajeno. */
  private UUID resolveAccessibleVenueId(UUID userId, UUID selectedVenueId) {
    return (selectedVenueId == null
            ? venueDao.findCurrentByOwnerUserId(userId)
            : venueDao.findAccessibleById(userId, selectedVenueId))
        .orElseThrow(VenueStatisticsNotFoundException::new)
        .getId();
  }

  private DateRange resolveRange(
      VenueStatisticsPeriod period, LocalDate fromDate, LocalDate toDate) {
    LocalDate today = LocalDate.now(clock);
    if (period != VenueStatisticsPeriod.CUSTOM && (fromDate != null || toDate != null)) {
      throw new VenueStatisticsFilterInvalidException();
    }
    DateRange range =
        switch (period) {
          case TODAY -> new DateRange(today, today);
          case WEEK ->
              new DateRange(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today);
          case MONTH -> new DateRange(today.withDayOfMonth(1), today);
          case YEAR -> new DateRange(today.withDayOfYear(1), today);
          case CUSTOM -> new DateRange(fromDate, toDate);
        };
    if (range.fromDate() == null
        || range.toDate() == null
        || range.fromDate().isAfter(range.toDate())
        || range.toDate().isAfter(today)
        || ChronoUnit.DAYS.between(range.fromDate(), range.toDate()) + 1 > MAX_RANGE_DAYS) {
      throw new VenueStatisticsFilterInvalidException();
    }
    return range;
  }

  private VenueStatisticsResponse toResponse(
      UUID venueId,
      VenueStatisticsPeriod period,
      DateRange range,
      List<StatsDailyVenueEntity> days) {
    long reservations = sum(days, Metric.RESERVATIONS);
    long confirmed = sum(days, Metric.CONFIRMED);
    long cancelled = sum(days, Metric.CANCELLED);
    long noShows = sum(days, Metric.NO_SHOW);
    long attended = sum(days, Metric.ATTENDED);
    long occupied = sum(days, Metric.OCCUPIED);
    long available = sum(days, Metric.AVAILABLE);
    long reviews = sum(days, Metric.REVIEWS);
    long incidents = sum(days, Metric.INCIDENTS);
    return new VenueStatisticsResponse(
        period.externalValue(),
        range.fromDate(),
        range.toDate(),
        reservations,
        confirmed,
        cancelled,
        noShows,
        attended,
        occupied,
        available,
        percentage(occupied, available),
        reviews,
        incidents,
        weightedRating(days, reviews),
        days.stream().map(this::toDaily).toList(),
        demandMetricsAssembler.assemble(
            venueId, range.fromDate(), range.toDate(), confirmed, clock.getZone()));
  }

  private VenueStatisticsDailyResponse toDaily(StatsDailyVenueEntity day) {
    return new VenueStatisticsDailyResponse(
        day.getDate(),
        day.getReservationsCount(),
        day.getConfirmedCount(),
        day.getCancelledCount(),
        day.getNoShowCount(),
        day.getAttendedCount(),
        day.getOccupiedCapacity(),
        day.getAvailableCapacity(),
        percentage(day.getOccupiedCapacity(), day.getAvailableCapacity()),
        day.getReviewsCount(),
        day.getIncidentsCount(),
        day.getAverageRating());
  }

  private long sum(List<StatsDailyVenueEntity> days, Metric metric) {
    return days.stream().mapToLong(metric::value).sum();
  }

  private BigDecimal percentage(long occupied, long available) {
    if (available == 0) {
      return BigDecimal.ZERO.setScale(1);
    }
    return BigDecimal.valueOf(occupied)
        .multiply(BigDecimal.valueOf(100))
        .divide(BigDecimal.valueOf(available), 1, RoundingMode.HALF_UP);
  }

  private BigDecimal weightedRating(List<StatsDailyVenueEntity> days, long reviews) {
    if (reviews == 0) {
      return null;
    }
    BigDecimal weightedSum =
        days.stream()
            .filter(day -> day.getAverageRating() != null && day.getReviewsCount() > 0)
            .map(day -> day.getAverageRating().multiply(BigDecimal.valueOf(day.getReviewsCount())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return weightedSum.divide(BigDecimal.valueOf(reviews), 2, RoundingMode.HALF_UP);
  }

  private record DateRange(LocalDate fromDate, LocalDate toDate) {}

  private enum Metric {
    RESERVATIONS {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getReservationsCount();
      }
    },
    CONFIRMED {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getConfirmedCount();
      }
    },
    CANCELLED {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getCancelledCount();
      }
    },
    NO_SHOW {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getNoShowCount();
      }
    },
    ATTENDED {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getAttendedCount();
      }
    },
    OCCUPIED {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getOccupiedCapacity();
      }
    },
    AVAILABLE {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getAvailableCapacity();
      }
    },
    REVIEWS {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getReviewsCount();
      }
    },
    INCIDENTS {
      @Override
      long value(StatsDailyVenueEntity day) {
        return day.getIncidentsCount();
      }
    };

    abstract long value(StatsDailyVenueEntity day);
  }
}
