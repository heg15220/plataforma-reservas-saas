package com.reserly.platform.notifications;

import java.net.URI;
import java.time.Instant;

/** Contrato de plantillas transaccionales tipadas y localizadas. */
public interface LocalizedEmailTemplateService {

  /** Renderiza el enlace de verificación de un solo uso. */
  RenderedEmailTemplate renderEmailVerification(String locale, URI actionUrl, Instant expiresAt);

  /** Renderiza el enlace de recuperación de contraseña de un solo uso. */
  RenderedEmailTemplate renderPasswordReset(String locale, URI actionUrl, Instant expiresAt);

  /** Renderiza todos los detalles que el usuario necesita para gestionar su reserva. */
  RenderedEmailTemplate renderReservationConfirmation(
      String locale, ReservationConfirmationTemplateData data);

  /** Renderiza para el local los datos operativos de una reserva recién confirmada. */
  RenderedEmailTemplate renderVenueReservationNotification(
      String locale, VenueReservationNotificationTemplateData data);

  /** Renderiza el aviso al local cuando el titular cancela su reserva. */
  RenderedEmailTemplate renderUserCancellationNotice(
      String locale, ReservationCancelledByUserTemplateData data);

  /** Renderiza el aviso al usuario con el motivo auditado de cancelación del local. */
  RenderedEmailTemplate renderVenueCancellationNotice(
      String locale, ReservationCancelledByVenueTemplateData data);
}
