package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormFieldCommand;
import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementación transaccional del CRUD de campos personalizados. */
@Service
public class ReservationFormFieldServiceImpl implements ReservationFormFieldService {
  private static final int MAX_LABEL_LENGTH = 160;
  private static final int MAX_KEY_LENGTH = 80;
  private static final int MAX_OPTIONS = 50;
  private static final int MAX_OPTION_LENGTH = 160;
  private static final String KEY_PATTERN = "^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$";

  private final VenueDao venueDao;
  private final ReservationFormFieldDao fieldDao;

  public ReservationFormFieldServiceImpl(VenueDao venueDao, ReservationFormFieldDao fieldDao) {
    this.venueDao = venueDao;
    this.fieldDao = fieldDao;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ReservationFormFieldEntity> list(UUID ownerUserId) {
    requireVenue(ownerUserId, false);
    return List.copyOf(fieldDao.findAllOwned(ownerUserId));
  }

  @Override
  @Transactional
  public ReservationFormFieldEntity create(
      UUID ownerUserId, ReservationFormFieldCommand command) {
    VenueEntity venue = requireVenue(ownerUserId, true);
    int lastPosition = fieldDao.findLastActivePosition(venue.getId());
    if (lastPosition == Integer.MAX_VALUE) {
      throw new ReservationFormFieldInvalidException();
    }

    Instant now = Instant.now();
    ReservationFormFieldEntity field = new ReservationFormFieldEntity();
    field.setVenue(venue);
    field.setPosition(lastPosition + 1);
    field.setActive(true);
    field.setCreatedAt(now);
    applyEditableFields(field, command, now);
    return save(field);
  }

  @Override
  @Transactional
  public ReservationFormFieldEntity update(
      UUID ownerUserId, UUID fieldId, ReservationFormFieldCommand command) {
    requireVenue(ownerUserId, true);
    ReservationFormFieldEntity field =
        fieldDao
            .findOwnedForUpdate(ownerUserId, fieldId)
            .orElseThrow(ReservationFormFieldNotFoundException::new);
    applyEditableFields(field, command, Instant.now());
    return save(field);
  }

  @Override
  @Transactional
  public List<ReservationFormFieldEntity> reorder(UUID ownerUserId, List<UUID> fieldIds) {
    requireVenue(ownerUserId, true);
    List<ReservationFormFieldEntity> fields = fieldDao.findAllOwnedForUpdate(ownerUserId);
    validateCompleteOrder(fields, fieldIds);

    Map<UUID, ReservationFormFieldEntity> fieldsById = new HashMap<>();
    fields.forEach(field -> fieldsById.put(field.getId(), field));
    Instant now = Instant.now();
    List<ReservationFormFieldEntity> ordered = new ArrayList<>(fields.size());
    for (int position = 0; position < fieldIds.size(); position++) {
      ReservationFormFieldEntity field = fieldsById.get(fieldIds.get(position));
      field.setPosition(position);
      field.setUpdatedAt(now);
      ordered.add(field);
    }
    return saveAll(ordered);
  }

  @Override
  @Transactional
  public void delete(UUID ownerUserId, UUID fieldId) {
    requireVenue(ownerUserId, true);
    ReservationFormFieldEntity field =
        fieldDao
            .findOwnedForUpdate(ownerUserId, fieldId)
            .orElseThrow(ReservationFormFieldNotFoundException::new);
    try {
      fieldDao.delete(field);
      fieldDao.flush();
    } catch (DataIntegrityViolationException exception) {
      throw new ReservationFormFieldInvalidException(exception);
    }
  }

  /**
   * Actualiza los atributos configurables y mantiene la coherencia entre tipo y optionsJson antes de
   * llegar a los constraints de V21.
   */
  private void applyEditableFields(
      ReservationFormFieldEntity field, ReservationFormFieldCommand command, Instant updatedAt) {
    if (command == null) {
      throw new ReservationFormFieldInvalidException();
    }
    String label = normalizeRequired(command.label(), MAX_LABEL_LENGTH);
    String key = normalizeRequired(command.key(), MAX_KEY_LENGTH);
    if (!key.matches(KEY_PATTERN)) {
      throw new ReservationFormFieldInvalidException();
    }
    ReservationFormFieldType type =
        ReservationFormFieldType.fromCode(command.type())
            .orElseThrow(ReservationFormFieldInvalidException::new);

    field.setLabel(label);
    field.setKey(key);
    field.setType(type);
    field.setRequired(command.required());
    field.setOptions(normalizeOptions(type, command.options()));
    field.setUpdatedAt(updatedAt);
  }

  /**
   * Un selector activo necesita opciones utilizables. Otros tipos no pueden conservar opciones
   * ocultas que contradigan su contrato o reaparezcan al cambiar de tipo.
   */
  private List<String> normalizeOptions(
      ReservationFormFieldType type, List<String> requestedOptions) {
    if (type != ReservationFormFieldType.SELECT) {
      if (requestedOptions != null && !requestedOptions.isEmpty()) {
        throw new ReservationFormFieldInvalidException();
      }
      return null;
    }
    if (requestedOptions == null
        || requestedOptions.isEmpty()
        || requestedOptions.size() > MAX_OPTIONS) {
      throw new ReservationFormFieldInvalidException();
    }

    List<String> normalizedOptions = new ArrayList<>(requestedOptions.size());
    Set<String> normalizedKeys = new HashSet<>();
    for (String option : requestedOptions) {
      String normalized = normalizeRequired(option, MAX_OPTION_LENGTH);
      if (!normalizedKeys.add(normalized.toLowerCase(Locale.ROOT))) {
        throw new ReservationFormFieldInvalidException();
      }
      normalizedOptions.add(normalized);
    }
    return List.copyOf(normalizedOptions);
  }

  private void validateCompleteOrder(
      List<ReservationFormFieldEntity> fields, List<UUID> fieldIds) {
    if (fieldIds == null
        || fieldIds.size() != fields.size()
        || fieldIds.stream().anyMatch(id -> id == null)) {
      throw new ReservationFormFieldInvalidException();
    }
    Set<UUID> requestedIds = new HashSet<>(fieldIds);
    Set<UUID> ownedIds = new HashSet<>();
    fields.forEach(field -> ownedIds.add(field.getId()));
    if (requestedIds.size() != fieldIds.size() || !requestedIds.equals(ownedIds)) {
      throw new ReservationFormFieldInvalidException();
    }
  }

  private String normalizeRequired(String value, int maxLength) {
    if (value == null) {
      throw new ReservationFormFieldInvalidException();
    }
    String normalized = value.trim();
    if (normalized.isBlank() || normalized.length() > maxLength) {
      throw new ReservationFormFieldInvalidException();
    }
    return normalized;
  }

  private VenueEntity requireVenue(UUID ownerUserId, boolean lock) {
    return (lock
            ? venueDao.findCurrentByOwnerUserIdForUpdate(ownerUserId)
            : venueDao.findCurrentByOwnerUserId(ownerUserId))
        .orElseThrow(ReservationFormFieldNotFoundException::new);
  }

  private ReservationFormFieldEntity save(ReservationFormFieldEntity field) {
    try {
      return fieldDao.saveAndFlush(field);
    } catch (DataIntegrityViolationException exception) {
      throw new ReservationFormFieldInvalidException(exception);
    }
  }

  private List<ReservationFormFieldEntity> saveAll(
      List<ReservationFormFieldEntity> fields) {
    try {
      return List.copyOf(fieldDao.saveAllAndFlush(fields));
    } catch (DataIntegrityViolationException exception) {
      throw new ReservationFormFieldInvalidException(exception);
    }
  }
}
