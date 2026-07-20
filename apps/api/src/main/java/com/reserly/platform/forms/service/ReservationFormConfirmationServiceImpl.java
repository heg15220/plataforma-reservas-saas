package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormAnswerCommand;
import com.reserly.platform.forms.dto.ReservationFormFieldAnswer;
import com.reserly.platform.forms.dto.ReservationFormPreviewFieldResponse;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.dto.ValidatedReservationFormAnswer;
import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormResponseDao;
import com.reserly.platform.forms.persistence.ReservationFormResponseEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador de confirmación que reutiliza el validador de tipos de fase 6. */
@Service
public class ReservationFormConfirmationServiceImpl implements ReservationFormConfirmationService {

  private final ReservationFormFieldDao fieldDao;
  private final ReservationFormResponseDao responseDao;
  private final ReservationFormResponseValidator validator;

  public ReservationFormConfirmationServiceImpl(
      ReservationFormFieldDao fieldDao,
      ReservationFormResponseDao responseDao,
      ReservationFormResponseValidator validator) {
    this.fieldDao = fieldDao;
    this.responseDao = responseDao;
    this.validator = validator;
  }

  /**
   * Exige una transacción exterior para que respuestas y reserva se confirmen o reviertan juntas.
   */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public List<ValidatedReservationFormAnswer> validateAndPersist(
      UUID venueId,
      UUID reservationId,
      List<ReservationFormFieldAnswer> answers,
      Instant createdAt) {
    List<ReservationFormFieldEntity> fields = fieldDao.findAllPublishedByVenue(venueId);
    Map<UUID, ReservationFormFieldEntity> fieldsById = indexFields(fields);
    List<ReservationFormAnswerCommand> commands = toCommands(fieldsById, answers);
    List<ValidatedReservationFormAnswer> validated = validator.validate(toSchema(fields), commands);
    responseDao.saveAll(toEntities(reservationId, validated, createdAt));
    return validated;
  }

  private Map<UUID, ReservationFormFieldEntity> indexFields(
      List<ReservationFormFieldEntity> fields) {
    Map<UUID, ReservationFormFieldEntity> fieldsById = new HashMap<>();
    for (ReservationFormFieldEntity field : fields) {
      if (field == null || field.getId() == null || fieldsById.put(field.getId(), field) != null) {
        throw invalid(ReservationFormResponseViolation.INVALID_SCHEMA, null);
      }
    }
    return fieldsById;
  }

  private List<ReservationFormAnswerCommand> toCommands(
      Map<UUID, ReservationFormFieldEntity> fieldsById, List<ReservationFormFieldAnswer> answers) {
    if (answers == null) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, null);
    }
    Set<UUID> seen = new HashSet<>();
    if (answers.size() > fieldsById.size()) {
      throw invalid(ReservationFormResponseViolation.UNKNOWN_FIELD, null);
    }
    List<ReservationFormAnswerCommand> commands = new ArrayList<>(answers.size());
    for (ReservationFormFieldAnswer answer : answers) {
      UUID fieldId = answer == null ? null : answer.fieldId();
      if (fieldId == null) {
        throw invalid(ReservationFormResponseViolation.INVALID_VALUE, null);
      }
      if (!seen.add(fieldId)) {
        throw invalid(ReservationFormResponseViolation.DUPLICATE_FIELD, null);
      }
      ReservationFormFieldEntity field = fieldsById.get(fieldId);
      if (field == null) {
        throw invalid(ReservationFormResponseViolation.UNKNOWN_FIELD, null);
      }
      commands.add(new ReservationFormAnswerCommand(field.getKey(), answer.value()));
    }
    return commands;
  }

  private ReservationFormPreviewResponse toSchema(List<ReservationFormFieldEntity> fields) {
    List<ReservationFormPreviewFieldResponse> schema = new ArrayList<>(fields.size());
    for (int index = 0; index < fields.size(); index++) {
      ReservationFormFieldEntity field = fields.get(index);
      schema.add(
          new ReservationFormPreviewFieldResponse(
              field.getId(),
              "custom",
              field.getKey(),
              field.getType().code(),
              field.getLabel(),
              null,
              null,
              field.isRequired(),
              true,
              field.getOptions(),
              null,
              index));
    }
    return new ReservationFormPreviewResponse(schema);
  }

  private List<ReservationFormResponseEntity> toEntities(
      UUID reservationId, List<ValidatedReservationFormAnswer> answers, Instant createdAt) {
    return answers.stream()
        .map(
            answer -> {
              ReservationFormResponseEntity entity = new ReservationFormResponseEntity();
              entity.setReservationId(reservationId);
              entity.setFieldId(answer.fieldId());
              entity.setFieldKey(answer.fieldKey());
              entity.setFieldLabel(answer.fieldLabel());
              entity.setValue(answer.value());
              entity.setCreatedAt(createdAt);
              return entity;
            })
        .toList();
  }

  private ReservationFormResponseInvalidException invalid(
      ReservationFormResponseViolation violation, String key) {
    return new ReservationFormResponseInvalidException(violation, key);
  }
}
