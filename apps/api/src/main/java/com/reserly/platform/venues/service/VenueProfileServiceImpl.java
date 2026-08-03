package com.reserly.platform.venues.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.venues.dto.VenueProfileCommand;
import com.reserly.platform.venues.persistence.CategoryDao;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional del perfil privado, siempre acotada por el actor. */
@Service
public class VenueProfileServiceImpl implements VenueProfileService {

  private static final String DRAFT_STATUS = "draft";
  private static final String ARCHIVED_STATUS = "archived";
  private static final String AUTOMATIC_AVAILABILITY = "automatic";
  private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
  private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

  private final VenueDao venueDao;
  private final CategoryDao categoryDao;
  private final BusinessAccountDao businessAccountDao;
  private final VenueDescriptionService descriptionService;

  public VenueProfileServiceImpl(
      VenueDao venueDao,
      CategoryDao categoryDao,
      BusinessAccountDao businessAccountDao,
      VenueDescriptionService descriptionService) {
    this.venueDao = venueDao;
    this.categoryDao = categoryDao;
    this.businessAccountDao = businessAccountDao;
    this.descriptionService = descriptionService;
  }

  @Override
  @Transactional
  public VenueEntity create(UUID ownerUserId, VenueProfileCommand command) {
    return createProfile(ownerUserId, command, true);
  }

  @Override
  @Transactional
  public VenueEntity createAdditional(UUID ownerUserId, VenueProfileCommand command) {
    return createProfile(ownerUserId, command, false);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean canCreateAdditional(UUID userId) {
    return businessAccountDao
        .findByOwnerUserId(userId)
        .map(BusinessAccountEntity::isMultiVenueEnabled)
        .orElse(false);
  }

  private VenueEntity createProfile(
      UUID ownerUserId, VenueProfileCommand command, boolean singularEndpoint) {
    descriptionService.validate(command.descriptionI18n());
    validateCoordinates(command.latitude(), command.longitude());

    BusinessAccountEntity businessAccount =
        businessAccountDao
            .findByOwnerUserIdForVenueCreation(ownerUserId)
            .orElseThrow(VenueProfileForbiddenException::new);
    boolean hasCurrentVenue = venueDao.existsCurrentOwnedByUserId(ownerUserId);
    if (hasCurrentVenue && singularEndpoint) {
      throw new VenueProfileConflictException();
    }
    if (hasCurrentVenue && !businessAccount.isMultiVenueEnabled()) {
      throw new VenueProfileForbiddenException();
    }
    CategoryEntity category = requireActiveCategory(command.categoryId());
    Instant now = Instant.now();

    VenueEntity venue = new VenueEntity();
    venue.setOwnerUser(businessAccount.getOwnerUser());
    venue.setBusinessAccount(businessAccount);
    venue.setCategory(category);
    venue.setSlug(generateSlug(command.name()));
    venue.setStatus(DRAFT_STATUS);
    venue.setManualAvailabilityStatus(AUTOMATIC_AVAILABILITY);
    // Los campos base son un formulario completo y seguro. Se habilitan desde el alta para que la
    // primera publicación del local nunca exponga franjas que desemboquen en un proceso incompleto.
    venue.setReservationFormPublished(true);
    venue.setReservationFormFallbackApproved(false);
    venue.setCreatedAt(now);
    applyEditableFields(venue, command, now);

    try {
      return venueDao.saveAndFlush(venue);
    } catch (DataIntegrityViolationException exception) {
      throw new VenueProfileConflictException(exception);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueEntity> list(UUID userId) {
    return List.copyOf(venueDao.findAllAccessibleByUserId(userId));
  }

  @Override
  @Transactional(readOnly = true)
  public VenueEntity find(UUID userId, UUID venueId) {
    return venueDao
        .findAccessibleById(userId, venueId)
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  @Override
  @Transactional(readOnly = true)
  public VenueEntity find(UUID ownerUserId) {
    return venueDao
        .findCurrentByOwnerUserId(ownerUserId)
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  @Override
  @Transactional
  public VenueEntity update(UUID ownerUserId, VenueProfileCommand command) {
    descriptionService.validate(command.descriptionI18n());
    validateCoordinates(command.latitude(), command.longitude());
    VenueEntity venue =
        venueDao
            .findCurrentByOwnerUserIdForUpdate(ownerUserId)
            .orElseThrow(VenueProfileNotFoundException::new);
    CategoryEntity category = requireActiveCategory(command.categoryId());
    venue.setCategory(category);
    applyEditableFields(venue, command, Instant.now());
    try {
      return venueDao.saveAndFlush(venue);
    } catch (DataIntegrityViolationException exception) {
      throw new VenueProfileConflictException(exception);
    }
  }

  @Override
  @Transactional
  public VenueEntity update(UUID userId, UUID venueId, VenueProfileCommand command) {
    descriptionService.validate(command.descriptionI18n());
    validateCoordinates(command.latitude(), command.longitude());
    VenueEntity venue =
        venueDao
            .findAccessibleByIdForUpdate(userId, venueId)
            .orElseThrow(VenueProfileNotFoundException::new);
    venue.setCategory(requireActiveCategory(command.categoryId()));
    applyEditableFields(venue, command, Instant.now());
    try {
      return venueDao.saveAndFlush(venue);
    } catch (DataIntegrityViolationException exception) {
      throw new VenueProfileConflictException(exception);
    }
  }

  @Override
  @Transactional
  public void archive(UUID ownerUserId) {
    VenueEntity venue =
        venueDao
            .findCurrentByOwnerUserIdForUpdate(ownerUserId)
            .orElseThrow(VenueProfileNotFoundException::new);
    venue.setStatus(ARCHIVED_STATUS);
    venue.setUpdatedAt(Instant.now());
    venueDao.saveAndFlush(venue);
  }

  @Override
  @Transactional
  public void archive(UUID userId, UUID venueId) {
    VenueEntity venue =
        venueDao
            .findAccessibleByIdForUpdate(userId, venueId)
            .orElseThrow(VenueProfileNotFoundException::new);
    venue.setStatus(ARCHIVED_STATUS);
    venue.setUpdatedAt(Instant.now());
    venueDao.saveAndFlush(venue);
  }

  private CategoryEntity requireActiveCategory(UUID categoryId) {
    return categoryDao.findActiveById(categoryId).orElseThrow(VenueProfileInvalidException::new);
  }

  private void applyEditableFields(
      VenueEntity venue, VenueProfileCommand command, Instant updatedAt) {
    venue.setName(command.name().strip());
    venue.setDescriptionI18n(command.descriptionI18n());
    venue.setServicesI18n(command.servicesI18n());
    venue.setRulesI18n(command.rulesI18n());
    venue.setPublicTextI18n(command.publicTextI18n());
    venue.setDescription(sourceValue(command.descriptionI18n()));
    venue.setDefaultLocale(command.defaultLocale());
    venue.setContactEmail(normalizeEmail(command.contactEmail()));
    venue.setPhone(normalizeOptional(command.phone()));
    venue.setAddress(normalizeOptional(command.address()));
    venue.setCity(normalizeOptional(command.city()));
    venue.setProvince(normalizeOptional(command.province()));
    venue.setCountry(normalizeOptional(command.country()));
    venue.setPostalCode(normalizeOptional(command.postalCode()));
    venue.setLatitude(command.latitude());
    venue.setLongitude(command.longitude());
    venue.setShowPhone(command.showPhone());
    venue.setShowEmail(command.showEmail());
    venue.setUpdatedAt(updatedAt);
  }

  private String sourceValue(com.reserly.platform.localization.LocalizedText value) {
    if (value == null) {
      return null;
    }
    return value.resolve(value.sourceLocale()).orElseThrow(VenueProfileInvalidException::new);
  }

  private void validateCoordinates(BigDecimal latitude, BigDecimal longitude) {
    if ((latitude == null) != (longitude == null)) {
      throw new VenueProfileInvalidException();
    }
  }

  private String normalizeEmail(String value) {
    String normalized = normalizeOptional(value);
    return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }

  private String generateSlug(String name) {
    String decomposed = Normalizer.normalize(name.strip(), Normalizer.Form.NFD);
    String base =
        NON_SLUG
            .matcher(DIACRITICS.matcher(decomposed).replaceAll("").toLowerCase(Locale.ROOT))
            .replaceAll("-")
            .replaceAll("(^-|-$)", "");
    if (base.isBlank()) {
      base = "local";
    }
    int maxBaseLength = Math.min(base.length(), 160);
    return base.substring(0, maxBaseLength) + "-" + UUID.randomUUID().toString().substring(0, 8);
  }
}
