package com.reserly.platform.billing.persistence;

import com.reserly.platform.localization.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Plan SaaS administrable y localizado.
 *
 * <p>Los límites y funciones se almacenan como configuración declarativa. Esta entidad no aplica
 * feature gates por sí misma; los casos de uso consumidores deben interpretar claves conocidas.
 */
@Entity
@Table(name = "\"Plans\"")
public class PlanEntity {

  private UUID id;
  private String name;
  private LocalizedText nameI18n;
  private String slug;
  private BigDecimal priceMonthly;
  private BigDecimal priceYearly;
  private Map<String, Object> limitsJson;
  private List<String> featuresJson;
  private Map<String, Object> featuresI18nJson;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  /** Nombre canónico interno; la UI debe resolver siempre {@code nameI18n}. */
  @Column(name = "\"name\"", nullable = false, length = 120)
  public String getName() {
    return name;
  }

  public void setName(String value) {
    name = value;
  }

  /** Nombre público completo en español e inglés. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"nameI18n\"", nullable = false, columnDefinition = "jsonb")
  public LocalizedText getNameI18n() {
    return nameI18n;
  }

  public void setNameI18n(LocalizedText value) {
    nameI18n = value;
  }

  /** Identificador semántico estable e independiente de la traducción. */
  @Column(name = "\"slug\"", nullable = false, length = 64)
  public String getSlug() {
    return slug;
  }

  public void setSlug(String value) {
    slug = value;
  }

  @Column(name = "\"priceMonthly\"", nullable = false, precision = 12, scale = 2)
  public BigDecimal getPriceMonthly() {
    return priceMonthly;
  }

  public void setPriceMonthly(BigDecimal value) {
    priceMonthly = value;
  }

  @Column(name = "\"priceYearly\"", nullable = false, precision = 12, scale = 2)
  public BigDecimal getPriceYearly() {
    return priceYearly;
  }

  public void setPriceYearly(BigDecimal value) {
    priceYearly = value;
  }

  /** Límites por clave; un valor JSON {@code null} expresa ausencia de límite configurado. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"limitsJson\"", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> getLimitsJson() {
    return limitsJson;
  }

  public void setLimitsJson(Map<String, Object> value) {
    limitsJson = value == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(value));
  }

  /** Claves de funciones, nunca textos visibles. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"featuresJson\"", nullable = false, columnDefinition = "jsonb")
  public List<String> getFeaturesJson() {
    return featuresJson;
  }

  public void setFeaturesJson(List<String> value) {
    featuresJson = value == null ? null : List.copyOf(value);
  }

  /** Catálogo localizado indexado por las mismas claves de {@code featuresJson}. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"featuresI18nJson\"", nullable = false, columnDefinition = "jsonb")
  public Map<String, Object> getFeaturesI18nJson() {
    return featuresI18nJson;
  }

  public void setFeaturesI18nJson(Map<String, Object> value) {
    featuresI18nJson =
        value == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(value));
  }

  @Column(name = "\"isActive\"", nullable = false)
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean value) {
    active = value;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }

  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    updatedAt = value;
  }
}
