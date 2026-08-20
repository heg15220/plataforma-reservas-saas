package com.reserly.platform.demand.waitlist.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Entrada autoritativa de lista de espera con consentimiento y destino operativo explícitos. */
@Entity
@Table(name = "\"WaitlistEntries\"")
public class WaitlistEntryEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @Column(name = "\"venueId\"", nullable = false)
  private UUID venueId;

  @Column(name = "\"timeSlotId\"", nullable = false)
  private UUID timeSlotId;

  @Column(name = "\"serviceId\"")
  private UUID serviceId;

  @Column(name = "\"customerIdentityId\"")
  private UUID customerIdentityId;

  @Column(name = "\"contactEmail\"", nullable = false, length = 320)
  private String contactEmail;

  @Column(name = "\"contactEmailNormalized\"", nullable = false, length = 320)
  private String contactEmailNormalized;

  @Column(name = "\"customerLocale\"", nullable = false, length = 2)
  private String customerLocale;

  @Column(name = "\"partySize\"", nullable = false)
  private int partySize;

  @Column(name = "\"status\"", nullable = false, length = 24)
  private String status;

  @Column(name = "\"contactConsentVersion\"", nullable = false, length = 64)
  private String contactConsentVersion;

  @Column(name = "\"contactConsentedAt\"", nullable = false)
  private Instant contactConsentedAt;

  @Column(name = "\"contactRevokedAt\"")
  private Instant contactRevokedAt;

  @Column(name = "\"idempotencyKey\"", nullable = false, length = 128)
  private String idempotencyKey;

  @Column(name = "\"createdAt\"", nullable = false)
  private Instant createdAt;

  @Column(name = "\"updatedAt\"", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getVenueId() {
    return venueId;
  }

  public void setVenueId(UUID venueId) {
    this.venueId = venueId;
  }

  public UUID getTimeSlotId() {
    return timeSlotId;
  }

  public void setTimeSlotId(UUID timeSlotId) {
    this.timeSlotId = timeSlotId;
  }

  public UUID getServiceId() {
    return serviceId;
  }

  public void setServiceId(UUID serviceId) {
    this.serviceId = serviceId;
  }

  public UUID getCustomerIdentityId() {
    return customerIdentityId;
  }

  public void setCustomerIdentityId(UUID customerIdentityId) {
    this.customerIdentityId = customerIdentityId;
  }

  /** Email necesario para emitir la oferta; el motor recibe solo {@code contactSubjectId}. */
  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getContactEmailNormalized() {
    return contactEmailNormalized;
  }

  public void setContactEmailNormalized(String contactEmailNormalized) {
    this.contactEmailNormalized = contactEmailNormalized;
  }

  public String getCustomerLocale() {
    return customerLocale;
  }

  public void setCustomerLocale(String customerLocale) {
    this.customerLocale = customerLocale;
  }

  public int getPartySize() {
    return partySize;
  }

  public void setPartySize(int partySize) {
    this.partySize = partySize;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getContactConsentVersion() {
    return contactConsentVersion;
  }

  public void setContactConsentVersion(String contactConsentVersion) {
    this.contactConsentVersion = contactConsentVersion;
  }

  public Instant getContactConsentedAt() {
    return contactConsentedAt;
  }

  public void setContactConsentedAt(Instant contactConsentedAt) {
    this.contactConsentedAt = contactConsentedAt;
  }

  public Instant getContactRevokedAt() {
    return contactRevokedAt;
  }

  public void setContactRevokedAt(Instant contactRevokedAt) {
    this.contactRevokedAt = contactRevokedAt;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
