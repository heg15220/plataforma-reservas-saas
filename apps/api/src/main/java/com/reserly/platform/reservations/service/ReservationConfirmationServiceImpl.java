package com.reserly.platform.reservations.service;

import com.reserly.platform.availability.persistence.TimeSlotEntity;
import com.reserly.platform.forms.dto.ReservationFormFieldAnswer;
import com.reserly.platform.forms.dto.ValidatedReservationFormAnswer;
import com.reserly.platform.forms.service.ReservationFormConfirmationService;
import com.reserly.platform.forms.service.ReservationFormResponseInvalidException;
import com.reserly.platform.identity.service.OneTimeTokenService;
import com.reserly.platform.incidents.service.PenaltyService;
import com.reserly.platform.infrastructure.legal.LegalDocumentVersions;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.reservations.dto.ReservationConfirmRequest;
import com.reserly.platform.reservations.dto.ReservationConfirmResponse;
import com.reserly.platform.reservations.persistence.ReservationDao;
import com.reserly.platform.reservations.persistence.ReservationEntity;
import com.reserly.platform.reservations.persistence.ReservationTimeSlotDao;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Confirma el agregado, sus respuestas y su credencial de gestión bajo una sola transacción. */
@Service
public class ReservationConfirmationServiceImpl implements ReservationConfirmationService {

  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
  private static final String BOOKING_RULES_FALLBACK_ES =
      "Consulta con el local cualquier cambio o cancelación de la reserva.";
  private static final String BOOKING_RULES_FALLBACK_EN =
      "Contact the venue about any booking changes or cancellation.";

  private final ReservationDao reservationDao;
  private final ReservationTimeSlotDao timeSlotDao;
  private final OneTimeTokenService tokenService;
  private final ReservationHoldExpirationPolicy expirationPolicy;
  private final ReservationFormConfirmationService formConfirmationService;
  private final ReservationManagementTokenPolicy managementTokenPolicy;
  private final ApplicationEventPublisher eventPublisher;
  private final PenaltyService penaltyService;
  private final Clock clock;

  public ReservationConfirmationServiceImpl(
      ReservationDao reservationDao,
      ReservationTimeSlotDao timeSlotDao,
      OneTimeTokenService tokenService,
      ReservationHoldExpirationPolicy expirationPolicy,
      ReservationFormConfirmationService formConfirmationService,
      ReservationManagementTokenPolicy managementTokenPolicy,
      ApplicationEventPublisher eventPublisher,
      PenaltyService penaltyService,
      Clock clock) {
    this.reservationDao = reservationDao;
    this.timeSlotDao = timeSlotDao;
    this.tokenService = tokenService;
    this.expirationPolicy = expirationPolicy;
    this.formConfirmationService = formConfirmationService;
    this.managementTokenPolicy = managementTokenPolicy;
    this.eventPublisher = eventPublisher;
    this.penaltyService = penaltyService;
    this.clock = clock;
  }

  /**
   * Acredita el hold, bloquea la franja y confirma solo si capacidad, respuestas y credencial son
   * válidas. El evento contiene el secreto en claro únicamente en memoria y se releva tras commit.
   */
  @Override
  @Transactional
  public ReservationConfirmResponse confirm(UUID reservationId, ReservationConfirmRequest request) {
    validateRequest(reservationId, request);
    ReservationEntity reservation =
        reservationDao
            .findByIdForUpdate(reservationId)
            .orElseThrow(ReservationConfirmationInvalidException::new);
    Instant now = clock.instant();
    if (!"hold".equals(reservation.getStatus())
        || !tokenMatches(request.holdToken(), reservation.getHoldTokenHash())
        || request.partySize() != reservation.getPartySize()) {
      throw new ReservationConfirmationInvalidException();
    }
    if (reservation.getHoldExpiresAt() == null
        || !expirationPolicy.isActive(reservation.getHoldExpiresAt(), now)) {
      throw new ReservationHoldExpiredException();
    }
    String customerEmailNormalized = request.customerEmail().strip().toLowerCase(Locale.ROOT);
    penaltyService.requireBookingAllowed(customerEmailNormalized);
    TimeSlotEntity slot =
        timeSlotDao
            .findByIdForUpdate(reservation.getTimeSlot().getId())
            .orElseThrow(ReservationConfirmationInvalidException::new);
    long occupiedByOthers =
        reservationDao.sumOccupiedCapacityExcluding(slot.getId(), reservation.getId(), now);
    long capacityForOthers = (long) slot.getCapacity() - reservation.getPartySize();
    if (occupiedByOthers > capacityForOthers) {
      throw new ReservationCapacityUnavailableException();
    }

    List<ValidatedReservationFormAnswer> formAnswers;
    try {
      formAnswers =
          formConfirmationService.validateAndPersist(
              reservation.getVenue().getId(),
              reservation.getId(),
              request.formResponses().stream()
                  .map(answer -> new ReservationFormFieldAnswer(answer.fieldId(), answer.value()))
                  .toList(),
              now);
    } catch (ReservationFormResponseInvalidException exception) {
      throw new ReservationFormAnswersInvalidException(exception);
    }

    String managementToken = tokenService.generate();
    Instant managementTokenExpiresAt =
        managementTokenPolicy.expiresAt(
            reservation.getDate(), reservation.getEndsAt(), clock.getZone());
    String customerName = request.customerName().strip();
    String customerEmail = request.customerEmail().strip();
    reservation.setCustomerName(customerName);
    reservation.setCustomerEmail(customerEmail);
    reservation.setCustomerEmailNormalized(customerEmailNormalized);
    reservation.setCustomerLocale(
        SupportedLocale.fromLanguageTag(request.locale()).orElse(SupportedLocale.EN).languageTag());
    reservation.setStatus("confirmed");
    reservation.setHoldExpiresAt(null);
    reservation.setHoldTokenHash(null);
    reservation.setSecureTokenHash(tokenService.hash(managementToken));
    reservation.setSecureTokenExpiresAt(managementTokenExpiresAt);
    reservation.setPrivacyPolicyAcceptedAt(now);
    reservation.setPrivacyPolicyVersion(LegalDocumentVersions.PRIVACY_POLICY);
    reservation.setBookingRulesAcceptedAt(now);
    reservation.setBookingRulesSnapshot(resolvedBookingRules(reservation, request.locale()));
    reservation.setUpdatedAt(now);
    ReservationEntity saved = reservationDao.save(reservation);
    eventPublisher.publishEvent(
        confirmationEmailEvent(
            saved, formAnswers, request.locale(), managementToken, managementTokenExpiresAt));

    return new ReservationConfirmResponse(
        saved.getStatus(),
        saved.getId(),
        saved.getCustomerEmail(),
        saved.getVenue().getName(),
        saved.getDate(),
        saved.getStartsAt(),
        saved.getEndsAt(),
        saved.getPartySize());
  }

  private void validateRequest(UUID reservationId, ReservationConfirmRequest request) {
    if (reservationId == null
        || request == null
        || !request.acceptsPrivacyPolicy()
        || !request.acceptsBookingRules()
        || request.partySize() < 1
        || request.customerName() == null
        || request.customerName().isBlank()
        || request.customerName().strip().length() > 160
        || request.customerEmail() == null
        || request.customerEmail().isBlank()
        || request.customerEmail().strip().length() > 320
        || !EMAIL_PATTERN.matcher(request.customerEmail().strip()).matches()
        || request.formResponses() == null) {
      throw new ReservationConfirmationInvalidException();
    }
  }

  private ReservationConfirmationEmailRequestedEvent confirmationEmailEvent(
      ReservationEntity reservation,
      List<ValidatedReservationFormAnswer> formAnswers,
      String customerLocale,
      String managementToken,
      Instant managementTokenExpiresAt) {
    SupportedLocale resolvedCustomerLocale =
        SupportedLocale.fromLanguageTag(customerLocale).orElse(SupportedLocale.EN);
    SupportedLocale venueLocale =
        SupportedLocale.fromLanguageTag(reservation.getVenue().getDefaultLocale())
            .orElse(SupportedLocale.EN);
    String bookingRules = resolvedBookingRules(reservation, customerLocale);
    return new ReservationConfirmationEmailRequestedEvent(
        UUID.randomUUID(),
        reservation.getId(),
        reservation.getCustomerName(),
        reservation.getCustomerEmail(),
        reservation.getVenue().getName(),
        venueNotificationEmail(reservation),
        reservation.getVenue().getAddress(),
        resolvedCustomerLocale.languageTag(),
        venueLocale.languageTag(),
        reservation.getDate(),
        reservation.getStartsAt(),
        reservation.getEndsAt(),
        reservation.getPartySize(),
        bookingRules,
        managementToken,
        managementTokenExpiresAt,
        formAnswers.stream()
            .map(
                answer ->
                    new ReservationConfirmationEmailAnswer(
                        answer.fieldKey(), answer.fieldLabel(), answer.value().toString()))
            .toList());
  }

  /** Resuelve exactamente el texto que se muestra y conserva como evidencia de aceptación. */
  private String resolvedBookingRules(ReservationEntity reservation, String customerLocale) {
    SupportedLocale locale =
        SupportedLocale.fromLanguageTag(customerLocale).orElse(SupportedLocale.EN);
    String fallback =
        locale == SupportedLocale.ES ? BOOKING_RULES_FALLBACK_ES : BOOKING_RULES_FALLBACK_EN;
    return reservation.getVenue().getRulesI18n() == null
        ? fallback
        : reservation.getVenue().getRulesI18n().resolve(locale).orElse(fallback);
  }

  /** Usa el email operativo y garantiza fallback a la cuenta propietaria verificada. */
  private String venueNotificationEmail(ReservationEntity reservation) {
    String notificationEmail = reservation.getVenue().getNotificationEmail();
    if (notificationEmail != null && !notificationEmail.isBlank()) {
      return notificationEmail.strip();
    }
    String contactEmail = reservation.getVenue().getContactEmail();
    if (contactEmail != null && !contactEmail.isBlank()) {
      return contactEmail.strip();
    }
    return reservation.getVenue().getOwnerUser().getEmail();
  }

  private boolean tokenMatches(String rawToken, String storedHash) {
    if (!tokenService.isValid(rawToken) || storedHash == null) {
      return false;
    }
    String candidateHash = tokenService.hash(rawToken);
    return MessageDigest.isEqual(
        storedHash.getBytes(StandardCharsets.US_ASCII),
        candidateHash.getBytes(StandardCharsets.US_ASCII));
  }
}
