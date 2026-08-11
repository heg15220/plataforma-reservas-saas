package com.reserly.platform.identity.persistence;

import com.reserly.platform.identity.AccountType;
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
 * Cuenta con capacidad de autenticación en Reserly.
 *
 * <p>El email normalizado es la identidad única de acceso. Esta entidad no representa al cliente
 * anónimo del MVP y nunca debe exponerse directamente desde la API. El hash de contraseña debe
 * producirlo el servicio de credenciales; la entidad no acepta ni transforma secretos en claro.
 */
@Entity
@Table(name = "\"Users\"")
public class UserEntity {

  private UUID id;
  private String email;
  private String emailNormalized;
  private String passwordHash;
  private AccountType accountType;
  private String preferredLocale;
  private Instant emailVerifiedAt;
  private Instant legalTermsAcceptedAt;
  private String legalTermsVersion;
  private Instant privacyPolicyAcceptedAt;
  private String privacyPolicyVersion;
  private String status;
  private Instant createdAt;
  private Instant updatedAt;

  /** Identificador opaco de la cuenta. */
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "\"id\"", nullable = false)
  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  /** Email conservado para comunicación y presentación al titular. */
  @Column(name = "\"email\"", nullable = false, length = 320)
  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  /** Email canónico en minúsculas usado exclusivamente para unicidad y búsquedas. */
  @Column(name = "\"emailNormalized\"", nullable = false, length = 320)
  public String getEmailNormalized() {
    return emailNormalized;
  }

  public void setEmailNormalized(String emailNormalized) {
    this.emailNormalized = emailNormalized;
  }

  /** Hash robusto de contraseña; nunca contiene la contraseña original. */
  @Column(name = "\"passwordHash\"", nullable = false, length = 255)
  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  /**
   * Naturaleza de la cuenta. No sustituye los roles ni concede permisos por sí misma.
   *
   * <p>El registro empresarial debe establecer {@link AccountType#VENUE_BUSINESS} explícitamente.
   */
  @Convert(converter = AccountTypeConverter.class)
  @Column(name = "\"accountType\"", nullable = false, length = 32)
  public AccountType getAccountType() {
    return accountType;
  }

  public void setAccountType(AccountType accountType) {
    this.accountType = accountType;
  }

  /** Locale persistido de la cuenta, limitado por base de datos a {@code es} o {@code en}. */
  @Column(name = "\"preferredLocale\"", nullable = false, length = 2)
  public String getPreferredLocale() {
    return preferredLocale;
  }

  public void setPreferredLocale(String preferredLocale) {
    this.preferredLocale = preferredLocale;
  }

  /** Instante de verificación de email; {@code null} mientras la verificación esté pendiente. */
  @Column(name = "\"emailVerifiedAt\"")
  public Instant getEmailVerifiedAt() {
    return emailVerifiedAt;
  }

  public void setEmailVerifiedAt(Instant emailVerifiedAt) {
    this.emailVerifiedAt = emailVerifiedAt;
  }

  /** Evidencia mínima UTC de aceptación de las condiciones, sin IP ni user-agent. */
  @Column(name = "\"legalTermsAcceptedAt\"")
  public Instant getLegalTermsAcceptedAt() {
    return legalTermsAcceptedAt;
  }

  public void setLegalTermsAcceptedAt(Instant legalTermsAcceptedAt) {
    this.legalTermsAcceptedAt = legalTermsAcceptedAt;
  }

  /** Versión exacta de las condiciones aceptadas. */
  @Column(name = "\"legalTermsVersion\"", length = 32)
  public String getLegalTermsVersion() {
    return legalTermsVersion;
  }

  public void setLegalTermsVersion(String legalTermsVersion) {
    this.legalTermsVersion = legalTermsVersion;
  }

  /** Evidencia mínima UTC de aceptación de la política de privacidad. */
  @Column(name = "\"privacyPolicyAcceptedAt\"")
  public Instant getPrivacyPolicyAcceptedAt() {
    return privacyPolicyAcceptedAt;
  }

  public void setPrivacyPolicyAcceptedAt(Instant privacyPolicyAcceptedAt) {
    this.privacyPolicyAcceptedAt = privacyPolicyAcceptedAt;
  }

  /** Versión exacta de la política de privacidad aceptada. */
  @Column(name = "\"privacyPolicyVersion\"", length = 32)
  public String getPrivacyPolicyVersion() {
    return privacyPolicyVersion;
  }

  public void setPrivacyPolicyVersion(String privacyPolicyVersion) {
    this.privacyPolicyVersion = privacyPolicyVersion;
  }

  /** Estado operativo persistido y restringido por el esquema. */
  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  /** Instante UTC de creación. */
  @Column(name = "\"createdAt\"", nullable = false)
  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  /** Instante UTC de la última modificación. */
  @Column(name = "\"updatedAt\"", nullable = false)
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
