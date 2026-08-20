package com.reserly.platform.demand.attribution;

import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Clasifica evidencia observacional con precedencia cerrada y sin inferir causalidad.
 *
 * <p>Solo `recovered` y `generated` exigen que el evento señale el mismo local; eventos genéricos
 * de descubrimiento pueden justificar `assisted` porque describen el recorrido correlacionado
 * completo.
 */
@Component
public class BookingAttributionClassifier {

  private static final Set<String> GENERATED_TYPES =
      Set.of("recommendationShown", "promotionShown", "promotionOpened");
  private static final Set<String> ASSISTED_TYPES =
      Set.of(
          "searchPerformed",
          "categoryViewed",
          "venueImpression",
          "venueClicked",
          "filterApplied",
          "photosViewed",
          "reviewsViewed",
          "availabilityChecked");

  /** Aplica ventana, correlación de local y prioridad para devolver exactamente una clase. */
  public BookingAttributionDecision classify(
      UUID venueId, Instant confirmedAt, List<BehaviorEventEntity> events) {
    Instant windowStart = confirmedAt.minus(BookingAttributionPolicy.WINDOW);
    List<BehaviorEventEntity> eligible =
        events.stream()
            .filter(event -> !event.getOccurredAt().isBefore(windowStart))
            .filter(event -> !event.getOccurredAt().isAfter(confirmedAt))
            .sorted(
                Comparator.comparing(BehaviorEventEntity::getOccurredAt)
                    .thenComparing(BehaviorEventEntity::getEventId))
            .toList();

    List<BehaviorEventEntity> recovered = matchingVenue(eligible, venueId, Set.of("waitlistOffer"));
    if (!recovered.isEmpty()) {
      return decision("recovered", "WAITLIST_OFFER_RECORDED", 0.95, recovered);
    }
    List<BehaviorEventEntity> generated = matchingVenue(eligible, venueId, GENERATED_TYPES);
    if (!generated.isEmpty()) {
      return decision("generated", "RECOMMENDATION_OR_PROMOTION_SHOWN", 0.90, generated);
    }
    List<BehaviorEventEntity> assisted =
        eligible.stream().filter(event -> ASSISTED_TYPES.contains(event.getEventType())).toList();
    if (!assisted.isEmpty()) {
      return decision("assisted", "DISCOVERY_OR_COMPARISON_RECORDED", 0.70, assisted);
    }
    return decision("direct", "NO_DECISIVE_PLATFORM_SIGNAL", 0.50, List.of());
  }

  private static List<BehaviorEventEntity> matchingVenue(
      List<BehaviorEventEntity> events, UUID venueId, Set<String> types) {
    return events.stream()
        .filter(event -> venueId.equals(event.getVenueId()))
        .filter(event -> types.contains(event.getEventType()))
        .toList();
  }

  private static BookingAttributionDecision decision(
      String value, String reason, double confidence, List<BehaviorEventEntity> evidence) {
    return new BookingAttributionDecision(
        value,
        reason,
        confidence,
        evidence.stream().limit(BookingAttributionPolicy.MAX_EVIDENCE_ITEMS).toList());
  }
}
