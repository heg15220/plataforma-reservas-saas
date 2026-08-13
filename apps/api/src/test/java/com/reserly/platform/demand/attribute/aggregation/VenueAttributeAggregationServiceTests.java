package com.reserly.platform.demand.attribute.aggregation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.demand.attribute.persistence.VenueAttributeEvidenceDao;
import com.reserly.platform.demand.attribute.persistence.VenueAttributeEvidenceEntity;
import com.reserly.platform.demand.attribute.persistence.VenueAttributeProfileDao;
import com.reserly.platform.demand.attribute.persistence.VenueAttributeProfileEntity;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Verifica ponderación, desacuerdo, diversidad, volumen y decaimiento sin depender de PostgreSQL.
 */
class VenueAttributeAggregationServiceTests {
  private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

  @Test
  void combinesReliabilityConfidenceVolumeDiversityAgreementAndRecency() {
    UUID venueId = UUID.randomUUID();
    UUID attributeId = UUID.randomUUID();
    VenueAttributeEvidenceDao evidenceDao = mock(VenueAttributeEvidenceDao.class);
    VenueAttributeProfileDao profileDao = mock(VenueAttributeProfileDao.class);
    when(evidenceDao.findActive(venueId, attributeId, NOW))
        .thenReturn(
            List.of(
                evidence("verifiedAudit", 0.9, 1, 10, NOW.minus(Duration.ofDays(1))),
                evidence("venueDeclaration", 0.2, 0.8, 1, NOW.minus(Duration.ofDays(90))),
                evidence("customerAggregate", 0.8, 0.9, 20, NOW.minus(Duration.ofDays(5)))));
    when(profileDao.findByVenueIdAndAttributeId(venueId, attributeId)).thenReturn(Optional.empty());
    when(profileDao.saveAndFlush(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    DemandAggregationProperties properties =
        new DemandAggregationProperties(
            "weighted-v1",
            Duration.ofDays(90),
            20,
            Map.of(
                "venueDeclaration", 0.10,
                "structuredCatalog", 0.15,
                "operational", 0.20,
                "customerAggregate", 0.15,
                "verifiedAudit", 0.30,
                "imageAuxiliary", 0.10),
            new DemandAggregationProperties.ConfidenceFactors(0.25, 0.25, 0.30, 0.20));

    VenueAttributeProfileEntity result =
        new VenueAttributeAggregationService(
                evidenceDao, profileDao, properties, Clock.fixed(NOW, ZoneOffset.UTC))
            .aggregate(venueId, attributeId);

    assertThat(result.getScore()).isBetween(new BigDecimal("0.75"), new BigDecimal("0.90"));
    assertThat(result.getConfidence()).isBetween(new BigDecimal("0.60"), BigDecimal.ONE);
    assertThat(result.getSourceCount()).isEqualTo(3);
    assertThat(result.getEvidenceCount()).isEqualTo(3);
    assertThat(result.getSampleSize()).isEqualTo(31);
    assertThat(result.getAgreement()).isLessThan(BigDecimal.ONE);
    assertThat(result.getCalculationTrace()).containsKeys("algorithm", "evidenceIds", "weights");
    verify(profileDao).saveAndFlush(result);
  }

  private VenueAttributeEvidenceEntity evidence(
      String source, double score, double confidence, int sampleSize, Instant observedAt) {
    VenueAttributeEvidenceEntity item = new VenueAttributeEvidenceEntity();
    item.setId(UUID.randomUUID());
    item.setSourceType(source);
    item.setScore(BigDecimal.valueOf(score));
    item.setConfidence(BigDecimal.valueOf(confidence));
    item.setSampleSize(sampleSize);
    item.setObservedAt(observedAt);
    return item;
  }
}
