package com.reserly.platform.forms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.reserly.platform.forms.dto.ReservationFormAnswerCommand;
import com.reserly.platform.forms.dto.ReservationFormPreviewFieldResponse;
import com.reserly.platform.forms.dto.ReservationFormPreviewResponse;
import com.reserly.platform.forms.dto.ValidatedReservationFormAnswer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Implementación estricta por tipo que no depende del frontend ni de una reserva persistida. */
@Service
public class ReservationFormResponseValidatorImpl implements ReservationFormResponseValidator {
  private static final int MAX_SHORT_TEXT_LENGTH = 255;
  private static final int MAX_LONG_TEXT_LENGTH = 4000;
  private static final int MAX_EMAIL_LENGTH = 254;
  private static final int MAX_PHONE_LENGTH = 32;
  private static final Set<String> SUPPORTED_TYPES =
      Set.of(
          "short_text",
          "long_text",
          "number",
          "select",
          "checkbox",
          "date",
          "phone",
          "email",
          "time_slot");
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9][0-9 .()\\-]{6,30}$");

  @Override
  public List<ValidatedReservationFormAnswer> validate(
      ReservationFormPreviewResponse form, List<ReservationFormAnswerCommand> answers) {
    Map<String, ReservationFormPreviewFieldResponse> fieldsByKey = indexSchema(form);
    Map<String, JsonNode> answersByKey = indexAnswers(fieldsByKey, answers);
    List<ValidatedReservationFormAnswer> validated = new ArrayList<>();

    for (ReservationFormPreviewFieldResponse field : form.fields()) {
      JsonNode value = answersByKey.get(field.key());
      if (value == null || value.isNull()) {
        if (field.required()) {
          throw invalid(ReservationFormResponseViolation.MISSING_REQUIRED, field.key());
        }
        continue;
      }
      JsonNode normalized = validateValue(field, value);
      validated.add(
          new ValidatedReservationFormAnswer(
              field.id(),
              field.key(),
              field.label() == null ? field.labelKey() : field.label(),
              field.type(),
              normalized));
    }
    return List.copyOf(validated);
  }

  private Map<String, ReservationFormPreviewFieldResponse> indexSchema(
      ReservationFormPreviewResponse form) {
    if (form == null || form.fields() == null) {
      throw invalid(ReservationFormResponseViolation.INVALID_SCHEMA, null);
    }
    Map<String, ReservationFormPreviewFieldResponse> fieldsByKey = new HashMap<>();
    for (ReservationFormPreviewFieldResponse field : form.fields()) {
      if (field == null
          || field.key() == null
          || field.key().isBlank()
          || field.type() == null
          || !SUPPORTED_TYPES.contains(field.type())
          || !hasUsableLabel(field)
          || ("select".equals(field.type())
              && (field.options() == null || field.options().isEmpty()))
          || fieldsByKey.putIfAbsent(field.key(), field) != null) {
        throw invalid(
            ReservationFormResponseViolation.INVALID_SCHEMA, field == null ? null : field.key());
      }
    }
    return fieldsByKey;
  }

  private boolean hasUsableLabel(ReservationFormPreviewFieldResponse field) {
    return (field.label() != null && !field.label().isBlank())
        || (field.labelKey() != null && !field.labelKey().isBlank());
  }

  private Map<String, JsonNode> indexAnswers(
      Map<String, ReservationFormPreviewFieldResponse> fieldsByKey,
      List<ReservationFormAnswerCommand> answers) {
    Map<String, JsonNode> answersByKey = new HashMap<>();
    if (answers == null) {
      return answersByKey;
    }
    Set<String> seenKeys = new HashSet<>();
    for (ReservationFormAnswerCommand answer : answers) {
      String key = answer == null ? null : answer.key();
      if (key == null || key.isBlank()) {
        throw invalid(ReservationFormResponseViolation.INVALID_VALUE, key);
      }
      if (!seenKeys.add(key)) {
        throw invalid(ReservationFormResponseViolation.DUPLICATE_FIELD, key);
      }
      if (!fieldsByKey.containsKey(key)) {
        throw invalid(ReservationFormResponseViolation.UNKNOWN_FIELD, key);
      }
      answersByKey.put(key, answer.value());
    }
    return answersByKey;
  }

  private JsonNode validateValue(ReservationFormPreviewFieldResponse field, JsonNode value) {
    return switch (field.type()) {
      case "short_text" -> normalizedText(field.key(), value, MAX_SHORT_TEXT_LENGTH);
      case "long_text" -> normalizedText(field.key(), value, MAX_LONG_TEXT_LENGTH);
      case "number" -> normalizedNumber(field, value);
      case "select" -> normalizedSelect(field, value);
      case "checkbox" -> normalizedCheckbox(field.key(), value);
      case "date" -> normalizedDate(field.key(), value);
      case "phone" -> normalizedPhone(field.key(), value);
      case "email" -> normalizedEmail(field.key(), value);
      case "time_slot" -> normalizedTimeSlot(field.key(), value);
      default -> throw invalid(ReservationFormResponseViolation.INVALID_SCHEMA, field.key());
    };
  }

  private JsonNode normalizedText(String key, JsonNode value, int maxLength) {
    String normalized = requiredText(key, value);
    if (normalized.length() > maxLength) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, key);
    }
    return JsonNodeFactory.instance.textNode(normalized);
  }

  private JsonNode normalizedNumber(ReservationFormPreviewFieldResponse field, JsonNode value) {
    if (!value.isNumber()) {
      throw invalid(ReservationFormResponseViolation.INVALID_TYPE, field.key());
    }
    if ("party_size".equals(field.key())
        && (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 1)) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, field.key());
    }
    return value.deepCopy();
  }

  private JsonNode normalizedSelect(ReservationFormPreviewFieldResponse field, JsonNode value) {
    String normalized = requiredText(field.key(), value);
    if (field.options() == null || !field.options().contains(normalized)) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, field.key());
    }
    return JsonNodeFactory.instance.textNode(normalized);
  }

  private JsonNode normalizedCheckbox(String key, JsonNode value) {
    if (!value.isBoolean()) {
      throw invalid(ReservationFormResponseViolation.INVALID_TYPE, key);
    }
    return value.deepCopy();
  }

  private JsonNode normalizedDate(String key, JsonNode value) {
    String normalized = requiredText(key, value);
    try {
      LocalDate.parse(normalized);
    } catch (DateTimeException exception) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, key);
    }
    return JsonNodeFactory.instance.textNode(normalized);
  }

  private JsonNode normalizedPhone(String key, JsonNode value) {
    String normalized = requiredText(key, value);
    if (normalized.length() > MAX_PHONE_LENGTH || !PHONE_PATTERN.matcher(normalized).matches()) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, key);
    }
    return JsonNodeFactory.instance.textNode(normalized);
  }

  private JsonNode normalizedEmail(String key, JsonNode value) {
    String normalized = requiredText(key, value);
    if (normalized.length() > MAX_EMAIL_LENGTH || !EMAIL_PATTERN.matcher(normalized).matches()) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, key);
    }
    return JsonNodeFactory.instance.textNode(normalized);
  }

  private JsonNode normalizedTimeSlot(String key, JsonNode value) {
    String normalized = requiredText(key, value);
    try {
      UUID.fromString(normalized);
    } catch (IllegalArgumentException exception) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, key);
    }
    return JsonNodeFactory.instance.textNode(normalized);
  }

  private String requiredText(String key, JsonNode value) {
    if (!value.isTextual()) {
      throw invalid(ReservationFormResponseViolation.INVALID_TYPE, key);
    }
    String normalized = value.textValue().trim();
    if (normalized.isEmpty()) {
      throw invalid(ReservationFormResponseViolation.INVALID_VALUE, key);
    }
    return normalized;
  }

  private ReservationFormResponseInvalidException invalid(
      ReservationFormResponseViolation violation, String fieldKey) {
    return new ReservationFormResponseInvalidException(violation, fieldKey);
  }
}
