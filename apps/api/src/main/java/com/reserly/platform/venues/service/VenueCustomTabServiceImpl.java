package com.reserly.platform.venues.service;

import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.dto.VenueCustomTabCommand;
import com.reserly.platform.venues.persistence.VenueCustomTabDao;
import com.reserly.platform.venues.persistence.VenueCustomTabEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional del CRUD de pestañas siempre acotado por propietario. */
@Service
public class VenueCustomTabServiceImpl implements VenueCustomTabService {

  private static final int MAX_TABS = 16;
  private static final int MAX_TITLE_LENGTH = 80;
  private static final int MAX_CONTENT_LENGTH = 20_000;
  private static final String CONTENT_FORMAT = "safe_html";
  private static final Set<SupportedLocale> PUBLIC_LOCALES =
      Set.of(SupportedLocale.ES, SupportedLocale.EN);

  private final VenueDao venueDao;
  private final VenueCustomTabDao tabDao;
  private final VenueCustomTabHtmlSanitizer sanitizer;

  public VenueCustomTabServiceImpl(
      VenueDao venueDao, VenueCustomTabDao tabDao, VenueCustomTabHtmlSanitizer sanitizer) {
    this.venueDao = venueDao;
    this.tabDao = tabDao;
    this.sanitizer = sanitizer;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueCustomTabEntity> list(UUID ownerUserId) {
    requireVenue(ownerUserId, false);
    return List.copyOf(tabDao.findAllOwned(ownerUserId));
  }

  @Override
  @Transactional
  public VenueCustomTabEntity create(UUID ownerUserId, VenueCustomTabCommand command) {
    VenueEntity venue = requireVenue(ownerUserId, true);
    List<VenueCustomTabEntity> existing = tabDao.findAllOwned(ownerUserId);
    if (existing.size() >= MAX_TABS) {
      throw new VenueCustomTabLimitException();
    }
    Instant now = Instant.now();
    VenueCustomTabEntity tab = new VenueCustomTabEntity();
    tab.setVenue(venue);
    tab.setPosition(existing.size());
    tab.setContentFormat(CONTENT_FORMAT);
    tab.setCreatedAt(now);
    applyEditableFields(tab, command, now);
    return save(tab);
  }

  @Override
  @Transactional
  public VenueCustomTabEntity update(UUID ownerUserId, UUID tabId, VenueCustomTabCommand command) {
    requireVenue(ownerUserId, true);
    VenueCustomTabEntity tab =
        tabDao
            .findOwnedForUpdate(ownerUserId, tabId)
            .orElseThrow(VenueProfileNotFoundException::new);
    applyEditableFields(tab, command, Instant.now());
    return save(tab);
  }

  @Override
  @Transactional
  public List<VenueCustomTabEntity> reorder(UUID ownerUserId, List<UUID> tabIds) {
    requireVenue(ownerUserId, true);
    List<VenueCustomTabEntity> tabs = tabDao.findAllOwned(ownerUserId);
    if (!isExactPermutation(tabIds, tabs)) {
      throw new VenueCustomTabInvalidException();
    }
    Map<UUID, VenueCustomTabEntity> byId =
        tabs.stream()
            .collect(java.util.stream.Collectors.toMap(VenueCustomTabEntity::getId, tab -> tab));
    for (int position = 0; position < tabIds.size(); position++) {
      byId.get(tabIds.get(position)).setPosition(position);
      byId.get(tabIds.get(position)).setUpdatedAt(Instant.now());
    }
    saveAll(tabs);
    return tabDao.findAllOwned(ownerUserId);
  }

  @Override
  @Transactional
  public void delete(UUID ownerUserId, UUID tabId) {
    requireVenue(ownerUserId, true);
    VenueCustomTabEntity tab =
        tabDao
            .findOwnedForUpdate(ownerUserId, tabId)
            .orElseThrow(VenueProfileNotFoundException::new);
    tabDao.delete(tab);
    tabDao.flush();
    compactPositions(ownerUserId);
  }

  private void compactPositions(UUID ownerUserId) {
    List<VenueCustomTabEntity> remaining = tabDao.findAllOwned(ownerUserId);
    Instant now = Instant.now();
    for (int position = 0; position < remaining.size(); position++) {
      remaining.get(position).setPosition(position);
      remaining.get(position).setUpdatedAt(now);
    }
    saveAll(remaining);
  }

  private void applyEditableFields(
      VenueCustomTabEntity tab, VenueCustomTabCommand command, Instant updatedAt) {
    LocalizedText title = normalizeText(command.titleI18n(), true);
    LocalizedText content = normalizeText(command.contentI18n(), false);
    if (command.active()
        && (!title.hasRequiredTranslations(PUBLIC_LOCALES)
            || !content.hasRequiredTranslations(PUBLIC_LOCALES))) {
      throw new VenueCustomTabInvalidException();
    }
    tab.setTitleI18n(title);
    tab.setContentI18n(content);
    tab.setActive(command.active());
    tab.setUpdatedAt(updatedAt);
  }

  private LocalizedText normalizeText(LocalizedText value, boolean title) {
    if (value == null) {
      throw new VenueCustomTabInvalidException();
    }
    EnumMap<SupportedLocale, String> normalized = new EnumMap<>(SupportedLocale.class);
    for (Map.Entry<SupportedLocale, String> entry : value.values().entrySet()) {
      String text =
          title
              ? sanitizer.sanitizePlainText(entry.getValue())
              : sanitizer.sanitizeHtml(entry.getValue());
      validateLocalizedValue(text, title);
      normalized.put(entry.getKey(), text);
    }
    String sourceValue = normalized.get(value.sourceLocale());
    if (sourceValue == null || !hasVisibleValue(sourceValue, title)) {
      throw new VenueCustomTabInvalidException();
    }
    return new LocalizedText(value.sourceLocale(), normalized);
  }

  private void validateLocalizedValue(String text, boolean title) {
    int maxLength = title ? MAX_TITLE_LENGTH : MAX_CONTENT_LENGTH;
    if (text.length() > maxLength || !hasVisibleValue(text, title)) {
      throw new VenueCustomTabInvalidException();
    }
  }

  private boolean hasVisibleValue(String text, boolean title) {
    return title ? text != null && !text.isBlank() : sanitizer.hasVisibleText(text);
  }

  private boolean isExactPermutation(List<UUID> tabIds, List<VenueCustomTabEntity> tabs) {
    if (tabIds == null || tabIds.size() != tabs.size()) {
      return false;
    }
    Set<UUID> requested = new HashSet<>(tabIds);
    if (requested.size() != tabIds.size()) {
      return false;
    }
    Set<UUID> existing =
        tabs.stream().map(VenueCustomTabEntity::getId).collect(java.util.stream.Collectors.toSet());
    return existing.equals(requested);
  }

  private VenueEntity requireVenue(UUID ownerUserId, boolean lock) {
    return (lock
            ? venueDao.findCurrentByOwnerUserIdForUpdate(ownerUserId)
            : venueDao.findCurrentByOwnerUserId(ownerUserId))
        .orElseThrow(VenueProfileNotFoundException::new);
  }

  private VenueCustomTabEntity save(VenueCustomTabEntity tab) {
    try {
      return tabDao.saveAndFlush(tab);
    } catch (DataIntegrityViolationException exception) {
      throw new VenueCustomTabInvalidException(exception);
    }
  }

  private void saveAll(List<VenueCustomTabEntity> tabs) {
    try {
      tabDao.saveAllAndFlush(tabs);
    } catch (DataIntegrityViolationException exception) {
      throw new VenueCustomTabInvalidException(exception);
    }
  }
}
