package com.reserly.platform.forms.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Snapshot histórico de una respuesta ya validada durante la confirmación de reserva. */
@Entity
@Table(name = "\"ReservationFormResponses\"")
public class ReservationFormResponseEntity {

  private UUID id;
  private UUID reservationId;
  private UUID fieldId;
  private String fieldKey;
  private String fieldLabel;
  private JsonNode value;
  private Instant createdAt;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Reserva propietaria; se mantiene como UUID para evitar acoplamiento JPA entre módulos. */
  @Column(name = "\"reservationId\"", nullable = false)
  public UUID getReservationId() {
    return reservationId;
  }

  public void setReservationId(UUID reservationId) {
    this.reservationId = reservationId;
  }

  /** Campo original nullable: la FK usa ON DELETE SET NULL sin perder el snapshot. */
  @Column(name = "\"fieldId\"")
  public UUID getFieldId() {
    return fieldId;
  }

  public void setFieldId(UUID fieldId) {
    this.fieldId = fieldId;
  }

  @Column(name = "\"fieldKey\"", nullable = false, length = 80)
  public String getFieldKey() {
    return fieldKey;
  }

  public void setFieldKey(String fieldKey) {
    this.fieldKey = fieldKey;
  }

  @Column(name = "\"fieldLabel\"", nullable = false, length = 160)
  public String getFieldLabel() {
    return fieldLabel;
  }

  public void setFieldLabel(String fieldLabel) {
    this.fieldLabel = fieldLabel;
  }

  /** Valor JSON normalizado; nunca conserva referencias mutables al payload HTTP. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"valueJson\"", nullable = false, columnDefinition = "jsonb")
  public JsonNode getValue() {
    return value;
  }

  public void setValue(JsonNode value) {
    this.value = value == null ? null : value.deepCopy();
  }

  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
