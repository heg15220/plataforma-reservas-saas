package com.reserly.platform.forms.persistence;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.persistence.VenueEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Campo configurable de un local; los campos base obligatorios permanecen definidos en codigo. */
@Entity
@Table(name = "\"ReservationFormFields\"")
public class ReservationFormFieldEntity {
  private UUID id;
  private VenueEntity venue;
  private String label;
  private LocalizedText labelI18n;
  private String key;
  private ReservationFormFieldType type;
  private boolean required;
  private List<String> options;
  private List<LocalizedText> optionsI18n;
  private int position;
  private boolean active;
  private Instant createdAt;
  private Instant updatedAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** El propietario se deriva de la cuenta autenticada y nunca del payload. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity venue) {
    this.venue = venue;
  }

  @Column(name = "\"label\"", nullable = false, length = 160)
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  /** Label p?blico con idioma origen y valores ES/EN. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"labelI18n\"", columnDefinition = "jsonb")
  public LocalizedText getLabelI18n() {
    return labelI18n;
  }

  public void setLabelI18n(LocalizedText labelI18n) {
    this.labelI18n = labelI18n;
  }

  @Column(name = "\"key\"", nullable = false, length = 80)
  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Convert(converter = ReservationFormFieldTypeConverter.class)
  @Column(name = "\"type\"", nullable = false, length = 32)
  public ReservationFormFieldType getType() {
    return type;
  }

  public void setType(ReservationFormFieldType type) {
    this.type = type;
  }

  @Column(name = "\"isRequired\"", nullable = false)
  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  /** Opciones del select; debe ser null para el resto de tipos segun el constraint de V21. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"optionsJson\"", columnDefinition = "jsonb")
  public List<String> getOptions() {
    return options;
  }

  public void setOptions(List<String> options) {
    this.options = options == null ? null : new ArrayList<>(options);
  }

  /** Opciones localizadas alineadas por ?ndice con optionsJson. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"optionsI18nJson\"", columnDefinition = "jsonb")
  public List<LocalizedText> getOptionsI18n() {
    return optionsI18n;
  }

  public void setOptionsI18n(List<LocalizedText> optionsI18n) {
    this.optionsI18n = optionsI18n == null ? null : new ArrayList<>(optionsI18n);
  }

  @Column(name = "\"position\"", nullable = false)
  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  @Column(name = "\"isActive\"", nullable = false)
  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
