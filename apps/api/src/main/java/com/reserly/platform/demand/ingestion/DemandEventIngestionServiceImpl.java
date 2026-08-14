package com.reserly.platform.demand.ingestion;

import com.reserly.platform.demand.event.persistence.BehaviorEventDao;
import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import com.reserly.platform.demand.identity.persistence.AnonymousIdentityDao;
import com.reserly.platform.demand.identity.persistence.AnonymousIdentityEntity;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityDao;
import com.reserly.platform.demand.identity.persistence.CustomerIdentityEntity;
import com.reserly.platform.infrastructure.ratelimit.RateLimitScope;
import com.reserly.platform.infrastructure.ratelimit.RateLimitService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementación fail-closed del catálogo v1 con idempotencia física por {@code eventId}.
 *
 * <p>Primero valida todo el lote y solo después persiste cada elemento. Un conflicto concurrente de
 * unicidad se resuelve recuperando la fila vencedora; nunca se registra el evento ni su contexto.
 */
@Service
public class DemandEventIngestionServiceImpl implements DemandEventIngestionService {

  private static final int MAX_CONTEXT_BYTES = 4_096;
  private static final Map<String, EventDefinition> CATALOG = catalog();
  private static final Map<String, Set<String>> CONTEXT_KEYS = contextKeys();
  private static final Set<String> PURPOSES =
      Set.of("analytics", "personalization", "experimentation", "commercial_activation");
  private static final Pattern CODE = Pattern.compile("^[a-z][a-zA-Z0-9]{0,63}$");
  private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");

  private final BehaviorEventDao eventDao;
  private final AnonymousIdentityDao anonymousIdentityDao;
  private final CustomerIdentityDao customerIdentityDao;
  private final RateLimitService rateLimitService;
  private final DemandIngestionProperties properties;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  public DemandEventIngestionServiceImpl(
      BehaviorEventDao eventDao,
      AnonymousIdentityDao anonymousIdentityDao,
      CustomerIdentityDao customerIdentityDao,
      RateLimitService rateLimitService,
      DemandIngestionProperties properties,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry,
      Clock clock) {
    this.eventDao = eventDao;
    this.anonymousIdentityDao = anonymousIdentityDao;
    this.customerIdentityDao = customerIdentityDao;
    this.rateLimitService = rateLimitService;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
  }

  @Override
  public EventBatchIngestionResponse ingest(String producerId, EventBatchIngestionRequest request) {
    if (!properties.enabled()) {
      metric("disabled").increment();
      throw new DemandIngestionDisabledException();
    }
    if (!properties.serviceId().equals(producerId)) {
      reject("PRODUCER_INVALID");
    }
    rateLimitService.check(RateLimitScope.DEMAND_EVENT_INGESTION, producerId);
    return ingestValidated(request);
  }

  @Override
  public EventBatchIngestionResponse ingestTrusted(EventIngestionRequest request) {
    if (!properties.enabled()) {
      throw new DemandIngestionDisabledException();
    }
    return ingestValidated(new EventBatchIngestionRequest(List.of(request)));
  }

  private EventBatchIngestionResponse ingestValidated(EventBatchIngestionRequest request) {
    if (request.events().size() > properties.maximumBatchSize()) {
      reject("BATCH_TOO_LARGE");
    }

    Instant receivedAt = clock.instant();
    Set<UUID> batchIds = new HashSet<>();
    List<ValidatedEvent> validated = new ArrayList<>(request.events().size());
    for (EventIngestionRequest event : request.events()) {
      Timer.Sample validationTimer = Timer.start(meterRegistry);
      if (!batchIds.add(event.eventId())) {
        detailedRejection(event, "BATCH_DUPLICATE_ID");
        stopTimer(validationTimer, event, "validation", "rejected");
        reject("BATCH_DUPLICATE_ID");
      }
      try {
        validated.add(validate(event, receivedAt));
        stopTimer(validationTimer, event, "validation", "accepted");
      } catch (DemandIngestionException exception) {
        detailedRejection(event, exception.code());
        stopTimer(validationTimer, event, "validation", "rejected");
        throw exception;
      }
    }

    List<EventIngestionItemResponse> results = new ArrayList<>(validated.size());
    int accepted = 0;
    int duplicates = 0;
    for (ValidatedEvent event : validated) {
      Timer.Sample storageTimer = Timer.start(meterRegistry);
      if (eventDao.findByEventId(event.request().eventId()).isPresent()) {
        duplicates++;
        results.add(new EventIngestionItemResponse(event.request().eventId(), "duplicate"));
        metric("duplicate").increment();
        detailedOutcome(event.request(), "duplicate");
        stopTimer(storageTimer, event.request(), "storage", "duplicate");
        continue;
      }
      try {
        eventDao.saveAndFlush(toEntity(event, receivedAt));
        accepted++;
        results.add(new EventIngestionItemResponse(event.request().eventId(), "accepted"));
        metric("accepted").increment();
        detailedOutcome(event.request(), "accepted");
        stopTimer(storageTimer, event.request(), "storage", "accepted");
      } catch (DataIntegrityViolationException exception) {
        if (eventDao.findByEventId(event.request().eventId()).isEmpty()) {
          stopTimer(storageTimer, event.request(), "storage", "failed");
          throw exception;
        }
        duplicates++;
        results.add(new EventIngestionItemResponse(event.request().eventId(), "duplicate"));
        metric("duplicate").increment();
        detailedOutcome(event.request(), "duplicate");
        stopTimer(storageTimer, event.request(), "storage", "duplicate");
      }
    }
    return new EventBatchIngestionResponse(accepted, duplicates, List.copyOf(results));
  }

  private ValidatedEvent validate(EventIngestionRequest event, Instant receivedAt) {
    if (event.schemaVersion() != 1 || event.occurredAt().isAfter(receivedAt)) {
      reject("CONTRACT_INVALID");
    }
    EventDefinition definition = CATALOG.get(event.eventType());
    if (definition == null || !PURPOSES.contains(event.purpose())) {
      reject("CATALOG_INVALID");
    }
    validateIdentifiers(event, definition.allowedIds());
    validateContext(event.context(), definition.family());
    AnonymousIdentityEntity anonymous = validateAnonymousIdentity(event, receivedAt);
    CustomerIdentityEntity customer = validateCustomerIdentity(event, receivedAt);
    return new ValidatedEvent(event, definition, anonymous, customer);
  }

  private void validateIdentifiers(EventIngestionRequest event, Set<String> allowedIds) {
    Map<String, UUID> supplied = new HashMap<>();
    supplied.put("sessionId", event.sessionId());
    supplied.put("anonymousId", event.anonymousId());
    supplied.put("customerId", event.customerId());
    supplied.put("venueId", event.venueId());
    supplied.put("serviceId", event.serviceId());
    supplied.put("resourceId", event.resourceId());
    supplied.put("timeSlotId", event.timeSlotId());
    if (supplied.entrySet().stream()
        .anyMatch(entry -> entry.getValue() != null && !allowedIds.contains(entry.getKey()))) {
      reject("IDENTIFIER_NOT_ALLOWED");
    }
  }

  private void validateContext(Map<String, Object> context, String family) {
    Set<String> allowed = CONTEXT_KEYS.get(family);
    if (context.keySet().stream().anyMatch(key -> !allowed.contains(key))) {
      reject("CONTEXT_INVALID");
    }
    if (context.values().stream().anyMatch(this::unsupportedContextValue)) {
      reject("CONTEXT_INVALID");
    }
    validateContextValues(context);
    try {
      if (objectMapper.writeValueAsBytes(context).length > MAX_CONTEXT_BYTES) {
        reject("CONTEXT_TOO_LARGE");
      }
    } catch (JacksonException exception) {
      reject("CONTEXT_INVALID");
    }
  }

  private boolean unsupportedContextValue(Object value) {
    return value != null
        && !(value instanceof String)
        && !(value instanceof Number)
        && !(value instanceof Boolean);
  }

  private void validateContextValues(Map<String, Object> context) {
    validateInteger(context, "queryLength", 0, 256);
    validateInteger(context, "resultCount", 0, 1_000);
    validateInteger(context, "position", 1, 1_000);
    validateInteger(context, "distanceMeters", 0, 200_000);
    validateInteger(context, "itemCount", 0, 1_000);
    validateInteger(context, "availableSlotCount", 0, 1_000);
    validateInteger(context, "durationSeconds", 0, 86_400);
    validateInteger(context, "rating", 1, 5);
    validateInteger(context, "candidateCount", 0, 1_000);
    for (String key :
        List.of(
            "categoryCode",
            "approximateZone",
            "filterCode",
            "stepCode",
            "outcomeCode",
            "policyVersion",
            "explanationCode",
            "experimentKey",
            "variantKey",
            "modelVersion")) {
      validateCode(context, key);
    }
    validateDate(context, "availabilityDate");
    validateInstant(context, "expiresAt");
    validateUuid(context, "activationId");
    validateUuid(context, "rankingRequestId");
    validateMoney(context);
    Object isNewCustomer = context.get("isNewCustomer");
    if (isNewCustomer != null && !(isNewCustomer instanceof Boolean)) {
      reject("CONTEXT_INVALID");
    }
  }

  private void validateInteger(Map<String, Object> context, String key, int minimum, int maximum) {
    Object value = context.get(key);
    if (value == null) {
      return;
    }
    if (!(value instanceof Number)) {
      reject("CONTEXT_INVALID");
    }
    Number number = (Number) value;
    double numeric = number.doubleValue();
    if (numeric % 1 != 0 || numeric < minimum || numeric > maximum) {
      reject("CONTEXT_INVALID");
    }
  }

  private void validateCode(Map<String, Object> context, String key) {
    Object value = context.get(key);
    if (value != null && (!(value instanceof String code) || !CODE.matcher(code).matches())) {
      reject("CONTEXT_INVALID");
    }
  }

  private void validateDate(Map<String, Object> context, String key) {
    Object value = context.get(key);
    if (value == null) {
      return;
    }
    try {
      LocalDate.parse((String) value);
    } catch (ClassCastException | DateTimeParseException exception) {
      reject("CONTEXT_INVALID");
    }
  }

  private void validateInstant(Map<String, Object> context, String key) {
    Object value = context.get(key);
    if (value == null) {
      return;
    }
    try {
      Instant.parse((String) value);
    } catch (ClassCastException | DateTimeParseException exception) {
      reject("CONTEXT_INVALID");
    }
  }

  private void validateUuid(Map<String, Object> context, String key) {
    Object value = context.get(key);
    if (value == null) {
      return;
    }
    try {
      UUID.fromString((String) value);
    } catch (RuntimeException exception) {
      reject("CONTEXT_INVALID");
    }
  }

  private void validateMoney(Map<String, Object> context) {
    Object amount = context.get("amount");
    Object currency = context.get("currency");
    if ((amount == null) != (currency == null)) {
      reject("CONTEXT_INVALID");
    }
    if (amount == null) {
      return;
    }
    if (!(amount instanceof Number)
        || new BigDecimal(amount.toString()).signum() < 0
        || !(currency instanceof String code)
        || !CURRENCY.matcher(code).matches()) {
      reject("CONTEXT_INVALID");
    }
  }

  private AnonymousIdentityEntity validateAnonymousIdentity(
      EventIngestionRequest event, Instant now) {
    if (event.anonymousId() == null) {
      return null;
    }
    AnonymousIdentityEntity identity =
        anonymousIdentityDao
            .findById(event.anonymousId())
            .orElseThrow(() -> rejected("CONSENT_INVALID"));
    if (event.consentVersion() == null
        || !event.consentVersion().equals(identity.getPersonalizationConsentVersion())
        || identity.getPersonalizationRevokedAt() != null
        || !identity.getExpiresAt().isAfter(now)
        || !identity.getRetentionExpiresAt().isAfter(now)) {
      reject("CONSENT_INVALID");
    }
    return identity;
  }

  private CustomerIdentityEntity validateCustomerIdentity(
      EventIngestionRequest event, Instant now) {
    if (event.customerId() == null) {
      return null;
    }
    CustomerIdentityEntity identity =
        customerIdentityDao
            .findById(event.customerId())
            .orElseThrow(() -> rejected("CONSENT_INVALID"));
    if (event.consentVersion() == null
        || !event.consentVersion().equals(identity.getPersonalizationConsentVersion())
        || identity.getPersonalizationRevokedAt() != null
        || !identity.getRetentionExpiresAt().isAfter(now)) {
      reject("CONSENT_INVALID");
    }
    return identity;
  }

  private BehaviorEventEntity toEntity(ValidatedEvent validated, Instant receivedAt) {
    EventIngestionRequest event = validated.request();
    BehaviorEventEntity entity = new BehaviorEventEntity();
    entity.setEventId(event.eventId());
    entity.setSchemaVersion(event.schemaVersion());
    entity.setEventType(event.eventType());
    entity.setEventFamily(validated.definition().family());
    entity.setProducer(validated.definition().producer());
    entity.setPurpose(event.purpose());
    entity.setConsentVersion(event.consentVersion());
    entity.setOccurredAt(event.occurredAt());
    entity.setReceivedAt(receivedAt);
    entity.setRequestId(event.requestId());
    entity.setSessionId(event.sessionId());
    entity.setAnonymousIdentity(validated.anonymousIdentity());
    entity.setCustomerIdentity(validated.customerIdentity());
    entity.setVenueId(event.venueId());
    entity.setServiceId(event.serviceId());
    entity.setResourceId(event.resourceId());
    entity.setTimeSlotId(event.timeSlotId());
    entity.setCountryCode(event.countryCode());
    entity.setContextJson(new HashMap<>(event.context()));
    entity.setRetentionExpiresAt(receivedAt.plus(properties.retention()));
    entity.setCreatedAt(receivedAt);
    return entity;
  }

  private Counter metric(String result) {
    return Counter.builder("reserly.demand.events.ingestion")
        .tag("result", result)
        .register(meterRegistry);
  }

  /**
   * Catálogo efectivo utilizado por la ingesta; las pruebas de contrato lo comparan con JSON v1.
   */
  public static Set<String> supportedEventTypes() {
    return CATALOG.keySet();
  }

  private void detailedOutcome(EventIngestionRequest event, String result) {
    Counter.builder("reserly.demand.events.outcomes")
        .tag("eventType", safeEventType(event))
        .tag("schemaVersion", String.valueOf(event.schemaVersion()))
        .tag("result", result)
        .register(meterRegistry)
        .increment();
  }

  private void detailedRejection(EventIngestionRequest event, String code) {
    detailedOutcome(event, "rejected");
    Counter.builder("reserly.demand.events.rejections")
        .tag("eventType", safeEventType(event))
        .tag("schemaVersion", String.valueOf(event.schemaVersion()))
        .tag("code", code)
        .register(meterRegistry)
        .increment();
  }

  private void stopTimer(
      Timer.Sample sample, EventIngestionRequest event, String phase, String result) {
    sample.stop(
        Timer.builder("reserly.demand.events.latency")
            .tag("eventType", safeEventType(event))
            .tag("schemaVersion", String.valueOf(event.schemaVersion()))
            .tag("phase", phase)
            .tag("result", result)
            .register(meterRegistry));
  }

  private String safeEventType(EventIngestionRequest event) {
    String eventType = event.eventType();
    return eventType != null && CATALOG.containsKey(eventType) ? eventType : "unknown";
  }

  private void reject(String code) {
    metric("rejected").increment();
    Counter.builder("reserly.demand.events.rejected")
        .tag("code", code)
        .register(meterRegistry)
        .increment();
    throw rejected(code);
  }

  private DemandIngestionException rejected(String code) {
    return new DemandIngestionException(code);
  }

  private static Map<String, Set<String>> contextKeys() {
    return Map.of(
        "discovery",
            Set.of(
                "queryLength",
                "categoryCode",
                "resultCount",
                "position",
                "approximateZone",
                "distanceMeters"),
        "evaluation", Set.of("filterCode", "itemCount", "availabilityDate", "availableSlotCount"),
        "conversion",
            Set.of(
                "stepCode",
                "outcomeCode",
                "durationSeconds",
                "amount",
                "currency",
                "isNewCustomer"),
        "postBooking", Set.of("outcomeCode", "amount", "currency", "rating"),
        "activation",
            Set.of("activationId", "position", "policyVersion", "explanationCode", "expiresAt"),
        "experiment",
            Set.of(
                "experimentKey",
                "variantKey",
                "rankingRequestId",
                "policyVersion",
                "modelVersion",
                "candidateCount"));
  }

  private static Map<String, EventDefinition> catalog() {
    Map<String, EventDefinition> catalog = new HashMap<>();
    add(
        catalog,
        "discovery",
        "web",
        Set.of("sessionId", "anonymousId"),
        "searchPerformed",
        "categoryViewed");
    add(
        catalog,
        "discovery",
        "spring",
        Set.of("sessionId", "anonymousId", "venueId"),
        "venueImpression");
    add(catalog, "discovery", "web", Set.of("sessionId", "anonymousId", "venueId"), "venueClicked");
    add(catalog, "evaluation", "web", Set.of("sessionId", "anonymousId"), "filterApplied");
    add(
        catalog,
        "evaluation",
        "web",
        Set.of("sessionId", "anonymousId", "venueId"),
        "photosViewed",
        "reviewsViewed");
    add(
        catalog,
        "evaluation",
        "spring",
        Set.of("sessionId", "anonymousId", "venueId", "serviceId", "resourceId"),
        "availabilityChecked");
    add(
        catalog,
        "conversion",
        "spring",
        Set.of("sessionId", "anonymousId", "venueId", "serviceId", "timeSlotId"),
        "bookingStarted");
    add(
        catalog,
        "conversion",
        "web",
        Set.of("sessionId", "anonymousId", "venueId", "serviceId", "timeSlotId"),
        "bookingAbandoned");
    add(
        catalog,
        "conversion",
        "spring",
        Set.of("sessionId", "anonymousId", "customerId", "venueId", "serviceId", "timeSlotId"),
        "bookingCompleted");
    add(
        catalog,
        "postBooking",
        "spring",
        Set.of("customerId", "venueId", "serviceId", "timeSlotId"),
        "bookingCancelled",
        "attendanceConfirmed",
        "noShow");
    add(catalog, "postBooking", "spring", Set.of("customerId", "venueId"), "reviewSubmitted");
    add(
        catalog,
        "activation",
        "spring",
        Set.of("sessionId", "anonymousId", "customerId", "venueId"),
        "recommendationShown",
        "promotionShown");
    add(
        catalog,
        "activation",
        "web",
        Set.of("sessionId", "anonymousId", "customerId", "venueId"),
        "promotionOpened");
    add(
        catalog,
        "activation",
        "spring",
        Set.of("customerId", "venueId", "serviceId", "timeSlotId"),
        "waitlistOffer");
    add(
        catalog,
        "experiment",
        "spring",
        Set.of("sessionId", "anonymousId", "customerId"),
        "experimentAssigned",
        "rankingGenerated");
    add(
        catalog,
        "experiment",
        "demand-engine",
        Set.of("sessionId", "anonymousId", "customerId"),
        "modelVersionUsed");
    return Map.copyOf(catalog);
  }

  private static void add(
      Map<String, EventDefinition> catalog,
      String family,
      String producer,
      Set<String> allowedIds,
      String... eventTypes) {
    for (String eventType : eventTypes) {
      catalog.put(eventType, new EventDefinition(family, producer, allowedIds));
    }
  }

  private record EventDefinition(String family, String producer, Set<String> allowedIds) {}

  private record ValidatedEvent(
      EventIngestionRequest request,
      EventDefinition definition,
      AnonymousIdentityEntity anonymousIdentity,
      CustomerIdentityEntity customerIdentity) {}
}
