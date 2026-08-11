package com.reserly.platform.administration.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Evidencia inmutable y minimizada de una acción crítica.
 *
 * <p>Los snapshots solo describen transiciones de estado. El servicio rechaza valores incompletos y
 * el llamador es responsable de no incluir secretos ni PII no necesaria.
 */
@Entity
@Table(name = "\"AuditLogs\"")
public class AuditLogEntity {

  private UUID id;
  private UUID actorUserId;
  private String actorRole;
  private String entityType;
  private UUID entityId;
  private String action;
  private Map<String, Object> beforeJson;
  private Map<String, Object> afterJson;
  private String ipAddress;
  private String userAgent;
  private Instant createdAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID value) {
    id = value;
  }

  /** Actor humano; es nulo únicamente para procesos internos con rol {@code system}. */
  @Column(name = "\"actorUserId\"")
  public UUID getActorUserId() {
    return actorUserId;
  }

  public void setActorUserId(UUID value) {
    actorUserId = value;
  }

  @Column(name = "\"actorRole\"", nullable = false, length = 32)
  public String getActorRole() {
    return actorRole;
  }

  public void setActorRole(String value) {
    actorRole = value;
  }

  @Column(name = "\"entityType\"", nullable = false, length = 64)
  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String value) {
    entityType = value;
  }

  @Column(name = "\"entityId\"", nullable = false)
  public UUID getEntityId() {
    return entityId;
  }

  public void setEntityId(UUID value) {
    entityId = value;
  }

  @Column(name = "\"action\"", nullable = false, length = 64)
  public String getAction() {
    return action;
  }

  public void setAction(String value) {
    action = value;
  }

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"beforeJson\"", columnDefinition = "jsonb")
  public Map<String, Object> getBeforeJson() {
    return beforeJson;
  }

  public void setBeforeJson(Map<String, Object> value) {
    beforeJson = value == null ? null : Map.copyOf(value);
  }

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"afterJson\"", columnDefinition = "jsonb")
  public Map<String, Object> getAfterJson() {
    return afterJson;
  }

  public void setAfterJson(Map<String, Object> value) {
    afterJson = value == null ? null : Map.copyOf(value);
  }

  @Column(name = "\"ipAddress\"", length = 45)
  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String value) {
    ipAddress = value;
  }

  @Column(name = "\"userAgent\"", length = 500)
  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String value) {
    userAgent = value;
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }
}
