package com.reserly.platform.demand.waitlist.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Oferta persistida con identificador determinista y secreto de aceptación solo hasheado. */
@Entity
@Table(name = "\"WaitlistOffers\"")
public class WaitlistOfferEntity {

  @Id
  @Column(name = "\"id\"", nullable = false)
  private UUID id;

  @Column(name = "\"waitlistEntryId\"", nullable = false)
  private UUID waitlistEntryId;

  @Column(name = "\"allocationRequestId\"", nullable = false)
  private UUID allocationRequestId;

  @Column(name = "\"waveNumber\"", nullable = false)
  private int waveNumber;

  @Column(name = "\"position\"", nullable = false)
  private int position;

  @Column(name = "\"priorityScore\"", nullable = false)
  private long priorityScore;

  @Column(name = "\"status\"", nullable = false, length = 24)
  private String status;

  @Column(name = "\"availableAt\"", nullable = false)
  private Instant availableAt;

  @Column(name = "\"expiresAt\"", nullable = false)
  private Instant expiresAt;

  @Column(name = "\"offerTokenHash\"", nullable = false, length = 64)
  private String offerTokenHash;

  @Column(name = "\"acceptedReservationId\"")
  private UUID acceptedReservationId;

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

  public UUID getWaitlistEntryId() {
    return waitlistEntryId;
  }

  public void setWaitlistEntryId(UUID waitlistEntryId) {
    this.waitlistEntryId = waitlistEntryId;
  }

  public UUID getAllocationRequestId() {
    return allocationRequestId;
  }

  public void setAllocationRequestId(UUID allocationRequestId) {
    this.allocationRequestId = allocationRequestId;
  }

  public int getWaveNumber() {
    return waveNumber;
  }

  public void setWaveNumber(int waveNumber) {
    this.waveNumber = waveNumber;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public long getPriorityScore() {
    return priorityScore;
  }

  public void setPriorityScore(long priorityScore) {
    this.priorityScore = priorityScore;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getAvailableAt() {
    return availableAt;
  }

  public void setAvailableAt(Instant availableAt) {
    this.availableAt = availableAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  /** SHA-256 hexadecimal; el token original solo se entrega al trabajo de notificación. */
  public String getOfferTokenHash() {
    return offerTokenHash;
  }

  public void setOfferTokenHash(String offerTokenHash) {
    this.offerTokenHash = offerTokenHash;
  }

  public UUID getAcceptedReservationId() {
    return acceptedReservationId;
  }

  public void setAcceptedReservationId(UUID acceptedReservationId) {
    this.acceptedReservationId = acceptedReservationId;
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
