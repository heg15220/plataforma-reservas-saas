package com.reserly.platform.resources.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

/** Horario semanal basico de disponibilidad de un empleado, profesional o recurso reservable. */
@Entity
@Table(name = "\"EmployeeResourceHours\"")
public class EmployeeResourceHourEntity {

  private UUID id;
  private EmployeeResourceEntity employeeResource;
  private int weekday;
  private boolean available;
  private LocalTime startsAt;
  private LocalTime endsAt;
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

  /** Recurso propietario; se resuelve desde la ruta y la sesion, nunca desde el payload. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"employeeResourceId\"", nullable = false)
  public EmployeeResourceEntity getEmployeeResource() {
    return employeeResource;
  }

  public void setEmployeeResource(EmployeeResourceEntity employeeResource) {
    this.employeeResource = employeeResource;
  }

  @Column(name = "\"weekday\"", nullable = false)
  public int getWeekday() {
    return weekday;
  }

  public void setWeekday(int weekday) {
    this.weekday = weekday;
  }

  @Column(name = "\"isAvailable\"", nullable = false)
  public boolean isAvailable() {
    return available;
  }

  public void setAvailable(boolean available) {
    this.available = available;
  }

  @Column(name = "\"startsAt\"")
  public LocalTime getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(LocalTime startsAt) {
    this.startsAt = startsAt;
  }

  @Column(name = "\"endsAt\"")
  public LocalTime getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(LocalTime endsAt) {
    this.endsAt = endsAt;
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
