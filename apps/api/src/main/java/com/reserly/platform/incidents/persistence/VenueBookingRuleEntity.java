package com.reserly.platform.incidents.persistence;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.venues.persistence.VenueEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Configuración operativa única de cancelación, asistencia y confirmación del local. */
@Entity
@Table(name = "\"VenueBookingRules\"")
public class VenueBookingRuleEntity {

  private UUID id;
  private VenueEntity venue;
  private boolean cancellationAllowed = true;
  private int freeCancellationUntilMinutesBefore = 1440;
  private String noShowPolicyText;
  private LocalizedText noShowPolicyTextI18n;
  private String lateCancellationPolicyText;
  private LocalizedText lateCancellationPolicyTextI18n;
  private int autoMarkAttendedAfterMinutes = 120;
  private boolean requiresConfirmation;
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

  /** Local propietario; la unicidad física garantiza una sola configuración vigente. */
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"venueId\"", nullable = false)
  public VenueEntity getVenue() {
    return venue;
  }

  public void setVenue(VenueEntity value) {
    venue = value;
  }

  @Column(name = "\"cancellationAllowed\"", nullable = false)
  public boolean isCancellationAllowed() {
    return cancellationAllowed;
  }

  public void setCancellationAllowed(boolean value) {
    cancellationAllowed = value;
  }

  @Column(name = "\"freeCancellationUntilMinutesBefore\"", nullable = false)
  public int getFreeCancellationUntilMinutesBefore() {
    return freeCancellationUntilMinutesBefore;
  }

  public void setFreeCancellationUntilMinutesBefore(int value) {
    freeCancellationUntilMinutesBefore = value;
  }

  @Column(name = "\"noShowPolicyText\"", length = 2000)
  public String getNoShowPolicyText() {
    return noShowPolicyText;
  }

  public void setNoShowPolicyText(String value) {
    noShowPolicyText = value;
  }

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"noShowPolicyTextI18n\"", columnDefinition = "jsonb")
  public LocalizedText getNoShowPolicyTextI18n() {
    return noShowPolicyTextI18n;
  }

  public void setNoShowPolicyTextI18n(LocalizedText value) {
    noShowPolicyTextI18n = value;
  }

  @Column(name = "\"lateCancellationPolicyText\"", length = 2000)
  public String getLateCancellationPolicyText() {
    return lateCancellationPolicyText;
  }

  public void setLateCancellationPolicyText(String value) {
    lateCancellationPolicyText = value;
  }

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(
      name = "\"lateCancellationPolicyTextI18n\"",
      columnDefinition = "jsonb")
  public LocalizedText getLateCancellationPolicyTextI18n() {
    return lateCancellationPolicyTextI18n;
  }

  public void setLateCancellationPolicyTextI18n(LocalizedText value) {
    lateCancellationPolicyTextI18n = value;
  }

  @Column(name = "\"autoMarkAttendedAfterMinutes\"", nullable = false)
  public int getAutoMarkAttendedAfterMinutes() {
    return autoMarkAttendedAfterMinutes;
  }

  public void setAutoMarkAttendedAfterMinutes(int value) {
    autoMarkAttendedAfterMinutes = value;
  }

  @Column(name = "\"requiresConfirmation\"", nullable = false)
  public boolean isRequiresConfirmation() {
    return requiresConfirmation;
  }

  public void setRequiresConfirmation(boolean value) {
    requiresConfirmation = value;
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
