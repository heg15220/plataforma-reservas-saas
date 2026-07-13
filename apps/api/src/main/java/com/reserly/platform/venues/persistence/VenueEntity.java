package com.reserly.platform.venues.persistence;

import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.localization.LocalizedText;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Perfil de local gestionado por su propietario.
 *
 * <p>La entidad no concede publicación ni acepta cambios de propiedad. La imagen principal separa
 * la URL pública estable de la clave privada de almacenamiento.
 */
@Entity
@Table(name = "\"Venues\"")
public class VenueEntity {

  private UUID id;
  private UserEntity ownerUser;
  private BusinessAccountEntity businessAccount;
  private CategoryEntity category;
  private String name;
  private String slug;
  private String description;
  private LocalizedText descriptionI18n;
  private LocalizedText servicesI18n;
  private LocalizedText rulesI18n;
  private LocalizedText publicTextI18n;
  private String defaultLocale;
  private String contactEmail;
  private String phone;
  private String address;
  private String city;
  private String province;
  private String country;
  private String postalCode;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private String mainImageUrl;
  private String mainImageObjectKey;
  private String mainImageMediaType;
  private Long mainImageSizeBytes;
  private Integer mainImageWidth;
  private Integer mainImageHeight;
  private String status;
  private String manualAvailabilityStatus;
  private boolean showPhone;
  private boolean showEmail;
  private Instant publishedAt;
  private boolean reservationFormPublished;
  private boolean reservationFormFallbackApproved;
  private Instant reservationFormPublishedAt;
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

  /** Propietario autenticado. La relación compuesta del esquema valida también la cuenta. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"ownerUserId\"", nullable = false)
  public UserEntity getOwnerUser() {
    return ownerUser;
  }

  public void setOwnerUser(UserEntity ownerUser) {
    this.ownerUser = ownerUser;
  }

  /** Identidad empresarial del propietario; no puede elegirse desde la API. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"businessAccountId\"", nullable = false)
  public BusinessAccountEntity getBusinessAccount() {
    return businessAccount;
  }

  public void setBusinessAccount(BusinessAccountEntity businessAccount) {
    this.businessAccount = businessAccount;
  }

  /** Categoría activa seleccionada por el propietario. */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"categoryId\"", nullable = false)
  public CategoryEntity getCategory() {
    return category;
  }

  public void setCategory(CategoryEntity category) {
    this.category = category;
  }

  @Column(name = "\"name\"", nullable = false, length = 160)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /** Slug generado por servidor y no editable para preservar enlaces futuros. */
  @Column(name = "\"slug\"", nullable = false, length = 180)
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  @Column(name = "\"description\"")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  /** Descripción localizada; el campo canónico conserva el valor del idioma fuente. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"descriptionI18n\"", columnDefinition = "jsonb")
  public LocalizedText getDescriptionI18n() {
    return descriptionI18n;
  }

  public void setDescriptionI18n(LocalizedText descriptionI18n) {
    this.descriptionI18n = descriptionI18n;
  }

  /** Resumen localizado de servicios ofrecidos. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"servicesI18n\"", columnDefinition = "jsonb")
  public LocalizedText getServicesI18n() {
    return servicesI18n;
  }

  public void setServicesI18n(LocalizedText servicesI18n) {
    this.servicesI18n = servicesI18n;
  }

  /** Reglas públicas localizadas previas a reserva. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"rulesI18n\"", columnDefinition = "jsonb")
  public LocalizedText getRulesI18n() {
    return rulesI18n;
  }

  public void setRulesI18n(LocalizedText rulesI18n) {
    this.rulesI18n = rulesI18n;
  }

  /** Texto público libre y seguro para información adicional del perfil. */
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "\"publicTextI18n\"", columnDefinition = "jsonb")
  public LocalizedText getPublicTextI18n() {
    return publicTextI18n;
  }

  public void setPublicTextI18n(LocalizedText publicTextI18n) {
    this.publicTextI18n = publicTextI18n;
  }

  @Column(name = "\"defaultLocale\"", nullable = false, length = 2)
  public String getDefaultLocale() {
    return defaultLocale;
  }

  public void setDefaultLocale(String defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  @Column(name = "\"contactEmail\"", length = 320)
  public String getContactEmail() {
    return contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  @Column(name = "\"phone\"", length = 32)
  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  @Column(name = "\"address\"", length = 500)
  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  @Column(name = "\"city\"", length = 160)
  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  @Column(name = "\"province\"", length = 160)
  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  @Column(name = "\"country\"", length = 2)
  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  @Column(name = "\"postalCode\"", length = 24)
  public String getPostalCode() {
    return postalCode;
  }

  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }

  @Column(name = "\"latitude\"", precision = 9, scale = 6)
  public BigDecimal getLatitude() {
    return latitude;
  }

  public void setLatitude(BigDecimal latitude) {
    this.latitude = latitude;
  }

  @Column(name = "\"longitude\"", precision = 9, scale = 6)
  public BigDecimal getLongitude() {
    return longitude;
  }

  public void setLongitude(BigDecimal longitude) {
    this.longitude = longitude;
  }

  /** URL pública estable; la clave privada de objeto se mantiene separada. */
  @Column(name = "\"mainImageUrl\"", length = 1024)
  public String getMainImageUrl() {
    return mainImageUrl;
  }

  public void setMainImageUrl(String mainImageUrl) {
    this.mainImageUrl = mainImageUrl;
  }

  @Column(name = "\"mainImageObjectKey\"", length = 500)
  public String getMainImageObjectKey() {
    return mainImageObjectKey;
  }

  public void setMainImageObjectKey(String mainImageObjectKey) {
    this.mainImageObjectKey = mainImageObjectKey;
  }

  @Column(name = "\"mainImageMediaType\"", length = 32)
  public String getMainImageMediaType() {
    return mainImageMediaType;
  }

  public void setMainImageMediaType(String mainImageMediaType) {
    this.mainImageMediaType = mainImageMediaType;
  }

  @Column(name = "\"mainImageSizeBytes\"")
  public Long getMainImageSizeBytes() {
    return mainImageSizeBytes;
  }

  public void setMainImageSizeBytes(Long mainImageSizeBytes) {
    this.mainImageSizeBytes = mainImageSizeBytes;
  }

  @Column(name = "\"mainImageWidth\"")
  public Integer getMainImageWidth() {
    return mainImageWidth;
  }

  public void setMainImageWidth(Integer mainImageWidth) {
    this.mainImageWidth = mainImageWidth;
  }

  @Column(name = "\"mainImageHeight\"")
  public Integer getMainImageHeight() {
    return mainImageHeight;
  }

  public void setMainImageHeight(Integer mainImageHeight) {
    this.mainImageHeight = mainImageHeight;
  }

  /** Estado editorial controlado por casos de uso; el CRUD solo crea borradores y archiva. */
  @Column(name = "\"status\"", nullable = false, length = 32)
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @Column(name = "\"manualAvailabilityStatus\"", nullable = false, length = 32)
  public String getManualAvailabilityStatus() {
    return manualAvailabilityStatus;
  }

  public void setManualAvailabilityStatus(String manualAvailabilityStatus) {
    this.manualAvailabilityStatus = manualAvailabilityStatus;
  }

  @Column(name = "\"showPhone\"", nullable = false)
  public boolean isShowPhone() {
    return showPhone;
  }

  public void setShowPhone(boolean showPhone) {
    this.showPhone = showPhone;
  }

  @Column(name = "\"showEmail\"", nullable = false)
  public boolean isShowEmail() {
    return showEmail;
  }

  public void setShowEmail(boolean showEmail) {
    this.showEmail = showEmail;
  }

  @Column(name = "\"publishedAt\"")
  public Instant getPublishedAt() {
    return publishedAt;
  }

  public void setPublishedAt(Instant publishedAt) {
    this.publishedAt = publishedAt;
  }

  @Column(name = "\"reservationFormPublished\"", nullable = false)
  public boolean isReservationFormPublished() {
    return reservationFormPublished;
  }

  public void setReservationFormPublished(boolean reservationFormPublished) {
    this.reservationFormPublished = reservationFormPublished;
  }

  @Column(name = "\"reservationFormFallbackApproved\"", nullable = false)
  public boolean isReservationFormFallbackApproved() {
    return reservationFormFallbackApproved;
  }

  public void setReservationFormFallbackApproved(boolean approved) {
    this.reservationFormFallbackApproved = approved;
  }

  @Column(name = "\"reservationFormPublishedAt\"")
  public Instant getReservationFormPublishedAt() {
    return reservationFormPublishedAt;
  }

  public void setReservationFormPublishedAt(Instant publishedAt) {
    this.reservationFormPublishedAt = publishedAt;
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
