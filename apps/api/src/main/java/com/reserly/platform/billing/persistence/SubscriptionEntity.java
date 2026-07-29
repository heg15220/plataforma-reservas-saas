package com.reserly.platform.billing.persistence;

import com.reserly.platform.billing.BillingPeriod;
import com.reserly.platform.billing.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Suscripción actual de un local.
 *
 * <p>Las referencias se mantienen escalares para que los locks y actualizaciones de pago no carguen
 * el perfil completo. La base de datos garantiza una sola suscripción por local y coherencia de
 * fechas terminales.
 */
@Entity
@Table(name = "\"Subscriptions\"")
public class SubscriptionEntity {

  private UUID id;
  private UUID venueId;
  private UUID planId;
  private SubscriptionStatus status;
  private BillingPeriod billingPeriod;
  private Instant currentPeriodStartsAt;
  private Instant currentPeriodEndsAt;
  private Instant trialEndsAt;
  private Instant cancelledAt;
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

  @Column(name = "\"venueId\"", nullable = false)
  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID value) {
    venueId = value;
  }

  @Column(name = "\"planId\"", nullable = false)
  public UUID getPlanId() {
    return planId;
  }

  public void setPlanId(UUID value) {
    planId = value;
  }

  @Convert(converter = SubscriptionStatusConverter.class)
  @Column(name = "\"status\"", nullable = false, length = 32)
  public SubscriptionStatus getStatus() {
    return status;
  }

  public void setStatus(SubscriptionStatus value) {
    status = value;
  }

  @Convert(converter = BillingPeriodConverter.class)
  @Column(name = "\"billingPeriod\"", nullable = false, length = 16)
  public BillingPeriod getBillingPeriod() {
    return billingPeriod;
  }

  public void setBillingPeriod(BillingPeriod value) {
    billingPeriod = value;
  }

  @Column(name = "\"currentPeriodStartsAt\"")
  public Instant getCurrentPeriodStartsAt() {
    return currentPeriodStartsAt;
  }

  public void setCurrentPeriodStartsAt(Instant value) {
    currentPeriodStartsAt = value;
  }

  @Column(name = "\"currentPeriodEndsAt\"")
  public Instant getCurrentPeriodEndsAt() {
    return currentPeriodEndsAt;
  }

  public void setCurrentPeriodEndsAt(Instant value) {
    currentPeriodEndsAt = value;
  }

  @Column(name = "\"trialEndsAt\"")
  public Instant getTrialEndsAt() {
    return trialEndsAt;
  }

  public void setTrialEndsAt(Instant value) {
    trialEndsAt = value;
  }

  @Column(name = "\"cancelledAt\"")
  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(Instant value) {
    cancelledAt = value;
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
