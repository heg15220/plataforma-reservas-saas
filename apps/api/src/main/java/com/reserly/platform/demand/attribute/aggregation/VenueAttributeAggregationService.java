package com.reserly.platform.demand.attribute.aggregation;

import com.reserly.platform.demand.attribute.persistence.VenueAttributeEvidenceDao;
import com.reserly.platform.demand.attribute.persistence.VenueAttributeEvidenceEntity;
import com.reserly.platform.demand.attribute.persistence.VenueAttributeProfileDao;
import com.reserly.platform.demand.attribute.persistence.VenueAttributeProfileEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Combina evidencias vigentes mediante fiabilidad, confianza y decaimiento exponencial.
 *
 * <p>El score es una media ponderada. La confianza baja cuando hay desacuerdo, poca diversidad,
 * poco volumen o evidencia antigua. Una declaración del local nunca domina por sí sola y una imagen
 * auxiliar tiene el menor peso. La traza enumera cada evidencia y peso efectivo para reproducir el
 * resultado sin sobrescribir contradicciones.
 */
@Service
public class VenueAttributeAggregationService {
  private final VenueAttributeEvidenceDao evidenceDao;
  private final VenueAttributeProfileDao profileDao;
  private final DemandAggregationProperties properties;
  private final Clock clock;

  public VenueAttributeAggregationService(
      VenueAttributeEvidenceDao evidenceDao,
      VenueAttributeProfileDao profileDao,
      DemandAggregationProperties properties,
      Clock clock) {
    this.evidenceDao = evidenceDao;
    this.profileDao = profileDao;
    this.properties = properties;
    this.clock = clock;
  }

  /** Recalcula atómicamente la proyección; falla si no existe evidencia vigente. */
  @Transactional
  public VenueAttributeProfileEntity aggregate(UUID venueId, UUID attributeId) {
    Instant now = clock.instant();
    List<VenueAttributeEvidenceEntity> evidence = evidenceDao.findActive(venueId, attributeId, now);
    if (evidence.isEmpty()) {
      throw new IllegalStateException("No existe evidencia vigente para agregar");
    }
    List<WeightedEvidence> weighted = evidence.stream().map(item -> weight(item, now)).toList();
    double totalWeight = weighted.stream().mapToDouble(WeightedEvidence::weight).sum();
    double score =
        weighted.stream().mapToDouble(item -> item.score() * item.weight()).sum() / totalWeight;
    double agreement =
        1
            - Math.min(
                1,
                weighted.stream()
                        .mapToDouble(item -> item.weight() * Math.pow(item.score() - score, 2))
                        .sum()
                    / totalWeight
                    * 4);
    int sourceCount =
        (int) evidence.stream().map(VenueAttributeEvidenceEntity::getSourceType).distinct().count();
    int sampleSize = evidence.stream().mapToInt(VenueAttributeEvidenceEntity::getSampleSize).sum();
    double diversity = sourceCount / (double) properties.sourceWeights().size();
    double volume = Math.min(1, sampleSize / (double) properties.volumeSaturation());
    double recency = weighted.stream().mapToDouble(WeightedEvidence::recency).average().orElse(0);
    var factors = properties.confidenceFactors();
    double confidence =
        clamp(
            diversity * factors.diversity()
                + volume * factors.volume()
                + agreement * factors.agreement()
                + recency * factors.recency());

    VenueAttributeProfileEntity profile =
        profileDao
            .findByVenueIdAndAttributeId(venueId, attributeId)
            .orElseGet(VenueAttributeProfileEntity::new);
    if (profile.getCreatedAt() == null) profile.setCreatedAt(now);
    profile.setVenueId(venueId);
    profile.setAttributeId(attributeId);
    profile.setScore(decimal(score));
    profile.setConfidence(decimal(confidence));
    profile.setSourceDiversity(decimal(diversity));
    profile.setAgreement(decimal(agreement));
    profile.setRecency(decimal(recency));
    profile.setEvidenceCount(evidence.size());
    profile.setSourceCount(sourceCount);
    profile.setSampleSize(sampleSize);
    profile.setCalculationVersion(properties.version());
    profile.setCalculationTrace(trace(weighted));
    profile.setLastEvidenceAt(
        evidence.stream()
            .map(VenueAttributeEvidenceEntity::getObservedAt)
            .max(Instant::compareTo)
            .orElseThrow());
    profile.setExpiresAt(
        evidence.stream()
            .map(VenueAttributeEvidenceEntity::getExpiresAt)
            .filter(java.util.Objects::nonNull)
            .min(Instant::compareTo)
            .orElse(null));
    profile.setLastCalculatedAt(now);
    profile.setUpdatedAt(now);
    return profileDao.saveAndFlush(profile);
  }

  private WeightedEvidence weight(VenueAttributeEvidenceEntity evidence, Instant now) {
    Double reliability = properties.sourceWeights().get(evidence.getSourceType());
    if (reliability == null)
      throw new IllegalArgumentException("Fuente sin fiabilidad configurada");
    double age = Math.max(0, Duration.between(evidence.getObservedAt(), now).toSeconds());
    double recency = Math.pow(0.5, age / properties.halfLife().toSeconds());
    double weight = reliability * evidence.getConfidence().doubleValue() * recency;
    return new WeightedEvidence(
        evidence.getId(),
        evidence.getSourceType(),
        evidence.getScore().doubleValue(),
        recency,
        weight);
  }

  private Map<String, Object> trace(List<WeightedEvidence> evidence) {
    Map<String, Object> trace = new LinkedHashMap<>();
    trace.put("algorithm", "weighted-mean-with-confidence-v1");
    trace.put("evidenceIds", evidence.stream().map(item -> item.id().toString()).toList());
    trace.put(
        "weights",
        evidence.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    item -> item.id().toString(),
                    item -> decimal(item.weight()),
                    (left, right) -> left,
                    LinkedHashMap::new)));
    trace.put("sourceWeights", properties.sourceWeights());
    trace.put("halfLifeSeconds", properties.halfLife().toSeconds());
    return trace;
  }

  private BigDecimal decimal(double value) {
    return BigDecimal.valueOf(clamp(value)).setScale(8, RoundingMode.HALF_UP);
  }

  private double clamp(double value) {
    return Math.max(0, Math.min(1, value));
  }

  private record WeightedEvidence(
      UUID id, String source, double score, double recency, double weight) {}
}
