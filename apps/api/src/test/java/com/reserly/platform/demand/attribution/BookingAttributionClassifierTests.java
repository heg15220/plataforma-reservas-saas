package com.reserly.platform.demand.attribution;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.demand.event.persistence.BehaviorEventEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Cubre precedencia, ventana, correlación de local y clasificación directa. */
class BookingAttributionClassifierTests {

  private static final Instant CONFIRMED_AT = Instant.parse("2026-08-20T09:00:00Z");
  private final BookingAttributionClassifier classifier = new BookingAttributionClassifier();

  @Test
  void recoveredPrecedesGeneratedAndAssistedForSameVenue() {
    UUID venueId = UUID.randomUUID();
    var result =
        classifier.classify(
            venueId,
            CONFIRMED_AT,
            List.of(
                event("searchPerformed", CONFIRMED_AT.minus(2, ChronoUnit.HOURS), null),
                event("recommendationShown", CONFIRMED_AT.minus(1, ChronoUnit.HOURS), venueId),
                event("waitlistOffer", CONFIRMED_AT.minus(30, ChronoUnit.MINUTES), venueId)));

    assertThat(result.attributionClass()).isEqualTo("recovered");
    assertThat(result.reasonCode()).isEqualTo("WAITLIST_OFFER_RECORDED");
    assertThat(result.evidence())
        .extracting(BehaviorEventEntity::getEventType)
        .containsExactly("waitlistOffer");
  }

  @Test
  void generatedRequiresTheChosenVenue() {
    UUID venueId = UUID.randomUUID();
    var result =
        classifier.classify(
            venueId,
            CONFIRMED_AT,
            List.of(
                event(
                    "recommendationShown",
                    CONFIRMED_AT.minus(1, ChronoUnit.HOURS),
                    UUID.randomUUID()),
                event("venueClicked", CONFIRMED_AT.minus(30, ChronoUnit.MINUTES), venueId)));

    assertThat(result.attributionClass()).isEqualTo("assisted");
    assertThat(result.reasonCode()).isEqualTo("DISCOVERY_OR_COMPARISON_RECORDED");
  }

  @Test
  void eventsOutsideWindowOrAfterConfirmationCannotInfluenceDecision() {
    UUID venueId = UUID.randomUUID();
    var result =
        classifier.classify(
            venueId,
            CONFIRMED_AT,
            List.of(
                event("waitlistOffer", CONFIRMED_AT.minus(8, ChronoUnit.DAYS), venueId),
                event("recommendationShown", CONFIRMED_AT.plusSeconds(1), venueId)));

    assertThat(result.attributionClass()).isEqualTo("direct");
    assertThat(result.evidence()).isEmpty();
  }

  private static BehaviorEventEntity event(String type, Instant occurredAt, UUID venueId) {
    BehaviorEventEntity event = new BehaviorEventEntity();
    event.setEventId(UUID.randomUUID());
    event.setEventType(type);
    event.setOccurredAt(occurredAt);
    event.setVenueId(venueId);
    return event;
  }
}
