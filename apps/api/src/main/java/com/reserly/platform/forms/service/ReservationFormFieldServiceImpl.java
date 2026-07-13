package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormFieldCommand;
import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implementacion transaccional del CRUD de campos personalizados. */
@Service
public class ReservationFormFieldServiceImpl implements ReservationFormFieldService {
  private static final int MAX_LABEL_LENGTH = 160;
  private static final int MAX_KEY_LENGTH = 80;
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
    field.setRequired(false);
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
  public void delete(UUID ownerUserId, UUID fieldId) {
    requireVenue(ownerUserId, true);
    ReservationFormFieldEntity field =
        fieldDao
            .findOwnedForUpdate(ownerUserId, fieldId)
            .orElseThrow(ReservationFormFieldNotFoundException::new);
    fieldDao.delete(field);
    fieldDao.flush();
  }

  /**
   * Aplica solo label, key y type. Obligatoriedad, opciones y posicion quedan reservadas a 6.5 y
   * 6.6; al cambiar de tipo mantiene el constraint de opciones de V21.
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

    if (type == ReservationFormFieldType.SELECT) {
      if (field.getType() != ReservationFormFieldType.SELECT || field.getOptions() == null) {
        field.setOptions(List.of());
      }
    } else {
      field.setOptions(null);
    }
    field.setLabel(label);
    field.setKey(key);
    field.setType(type);
    field.setUpdatedAt(updatedAt);
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
}
