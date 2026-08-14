package com.reserly.platform.demand.observability;

import com.reserly.platform.demand.ingestion.DemandEventIngestionServiceImpl;
import com.reserly.platform.demand.quality.DemandDatasetQualityReport;
import com.reserly.platform.demand.quality.DemandDatasetQualityService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Construye un dashboard interno sin consultar ni devolver dimensiones de usuario/local.
 *
 * <p>Volumen y cobertura respetan la ventana solicitada. Rechazos, duplicados y timers proceden de
 * Micrometer y abarcan la vida del proceso, alcance declarado explícitamente en la respuesta.
 */
@Service
public class DemandObservabilityService {
  private final JdbcTemplate jdbc;
  private final MeterRegistry registry;
  private final DemandDatasetQualityService qualityService;
  private final Clock clock;

  public DemandObservabilityService(
      JdbcTemplate jdbc,
      MeterRegistry registry,
      DemandDatasetQualityService qualityService,
      Clock clock) {
    this.jdbc = jdbc;
    this.registry = registry;
    this.qualityService = qualityService;
    this.clock = clock;
  }

  /** Genera el read model para una ventana validada por el controlador. */
  @Transactional(readOnly = true)
  public DemandObservabilityDashboard dashboard(Duration window) {
    Instant now = clock.instant();
    Instant start = now.minus(window);
    Map<EventVersion, Long> volumes = persistedVolumes(start);
    Set<EventVersion> keys = new HashSet<>(volumes.keySet());
    DemandEventIngestionServiceImpl.supportedEventTypes()
        .forEach(type -> keys.add(new EventVersion(type, (short) 1)));
    collectRuntimeKeys(keys);

    List<DemandEventMetric> events =
        keys.stream()
            .sorted(
                Comparator.comparing(EventVersion::eventType).thenComparing(EventVersion::version))
            .map(key -> metric(key, volumes.getOrDefault(key, 0L)))
            .toList();
    List<String> missing =
        DemandEventIngestionServiceImpl.supportedEventTypes().stream()
            .filter(type -> volumes.getOrDefault(new EventVersion(type, (short) 1), 0L) == 0)
            .sorted()
            .toList();
    int expected = DemandEventIngestionServiceImpl.supportedEventTypes().size();
    double coverage = expected == 0 ? 100.0 : 100.0 * (expected - missing.size()) / expected;
    DemandDatasetQualityReport quality = qualityService.audit(window);
    return new DemandObservabilityDashboard(
        now,
        start,
        "process_lifetime",
        volumes.values().stream().mapToLong(Long::longValue).sum(),
        coverage,
        missing,
        events,
        rejectionReasons(),
        quality);
  }

  private Map<EventVersion, Long> persistedVolumes(Instant start) {
    Map<EventVersion, Long> result = new HashMap<>();
    jdbc.query(
        """
        SELECT "eventType", "schemaVersion", count(*) AS volume
        FROM "BehaviorEvents" WHERE "receivedAt" >= ?
        GROUP BY "eventType", "schemaVersion"
        """,
        resultSet -> {
          while (resultSet.next()) {
            result.put(
                new EventVersion(resultSet.getString(1), resultSet.getShort(2)),
                resultSet.getLong(3));
          }
          return null;
        },
        Timestamp.from(start));
    return result;
  }

  private void collectRuntimeKeys(Set<EventVersion> keys) {
    for (Meter meter : registry.getMeters()) {
      if (!meter.getId().getName().startsWith("reserly.demand.events.")) continue;
      String eventType = meter.getId().getTag("eventType");
      String version = meter.getId().getTag("schemaVersion");
      if (eventType != null && version != null) {
        try {
          keys.add(new EventVersion(eventType, Short.parseShort(version)));
        } catch (NumberFormatException ignored) {
          // Una etiqueta ajena inválida no rompe el dashboard ni se refleja al cliente.
        }
      }
    }
  }

  private DemandEventMetric metric(EventVersion key, long volume) {
    long accepted = outcome(key, "accepted");
    long rejected = outcome(key, "rejected");
    long duplicates = outcome(key, "duplicate");
    long samples = 0;
    double totalSeconds = 0;
    double maxSeconds = 0;
    for (Meter meter : registry.getMeters()) {
      if (meter instanceof Timer timer
          && "reserly.demand.events.latency".equals(meter.getId().getName())
          && key.matches(meter.getId())) {
        samples += timer.count();
        totalSeconds += timer.totalTime(java.util.concurrent.TimeUnit.SECONDS);
        maxSeconds = Math.max(maxSeconds, timer.max(java.util.concurrent.TimeUnit.SECONDS));
      }
    }
    double meanMilliseconds = samples == 0 ? 0 : totalSeconds * 1_000 / samples;
    return new DemandEventMetric(
        key.eventType(),
        key.version(),
        volume,
        accepted,
        rejected,
        duplicates,
        samples,
        meanMilliseconds,
        maxSeconds * 1_000,
        volume > 0);
  }

  private long outcome(EventVersion key, String result) {
    Counter counter =
        registry
            .find("reserly.demand.events.outcomes")
            .tags(
                "eventType",
                key.eventType(),
                "schemaVersion",
                String.valueOf(key.version()),
                "result",
                result)
            .counter();
    return counter == null ? 0 : Math.round(counter.count());
  }

  private List<DemandRejectionMetric> rejectionReasons() {
    List<DemandRejectionMetric> result = new ArrayList<>();
    for (Counter counter : registry.find("reserly.demand.events.rejected").counters()) {
      String code = counter.getId().getTag("code");
      if (code != null) result.add(new DemandRejectionMetric(code, Math.round(counter.count())));
    }
    result.sort(Comparator.comparing(DemandRejectionMetric::code));
    return List.copyOf(result);
  }

  private record EventVersion(String eventType, short version) {
    private boolean matches(Meter.Id id) {
      return eventType.equals(id.getTag("eventType"))
          && String.valueOf(version).equals(id.getTag("schemaVersion"));
    }
  }
}
