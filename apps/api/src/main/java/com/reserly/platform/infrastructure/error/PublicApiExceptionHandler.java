package com.reserly.platform.infrastructure.error;

import com.reserly.platform.availability.controller.PublicVenueAvailabilityControllerImpl;
import com.reserly.platform.billing.controller.RedsysCallbackControllerImpl;
import com.reserly.platform.forms.controller.PublicReservationFormControllerImpl;
import com.reserly.platform.identity.controller.AuthenticationControllerImpl;
import com.reserly.platform.identity.controller.EmailVerificationControllerImpl;
import com.reserly.platform.identity.controller.PasswordResetControllerImpl;
import com.reserly.platform.identity.controller.VenueRegistrationControllerImpl;
import com.reserly.platform.reservations.controller.ReservationConfirmationControllerImpl;
import com.reserly.platform.reservations.controller.ReservationHoldControllerImpl;
import com.reserly.platform.reservations.controller.ReservationManagementControllerImpl;
import com.reserly.platform.reviews.controller.PublicVenueReviewControllerImpl;
import com.reserly.platform.reviews.controller.ReviewCreationControllerImpl;
import com.reserly.platform.venues.controller.VenueCategoryControllerImpl;
import com.reserly.platform.venues.controller.VenueGalleryControllerImpl;
import com.reserly.platform.venues.controller.VenueMainImageControllerImpl;
import com.reserly.platform.venues.controller.VenuePublicProfileControllerImpl;
import com.reserly.platform.venues.controller.VenuePublicSearchControllerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Cierra los fallos inesperados de superficies anónimas o callbacks externos.
 *
 * <p>Los manejadores de dominio más específicos conservan sus estados HTTP. Este fallback nunca
 * serializa {@link Exception#getMessage()}, causas, nombres de proveedor ni cuerpos remotos. El log
 * limita su diagnóstico al tipo de excepción para evitar que una librería externa introduzca datos
 * sensibles en texto libre.
 */
@RestControllerAdvice(
    assignableTypes = {
      PublicVenueAvailabilityControllerImpl.class,
      RedsysCallbackControllerImpl.class,
      PublicReservationFormControllerImpl.class,
      AuthenticationControllerImpl.class,
      EmailVerificationControllerImpl.class,
      PasswordResetControllerImpl.class,
      VenueRegistrationControllerImpl.class,
      ReservationConfirmationControllerImpl.class,
      ReservationHoldControllerImpl.class,
      ReservationManagementControllerImpl.class,
      PublicVenueReviewControllerImpl.class,
      ReviewCreationControllerImpl.class,
      VenueCategoryControllerImpl.class,
      VenueGalleryControllerImpl.class,
      VenueMainImageControllerImpl.class,
      VenuePublicProfileControllerImpl.class,
      VenuePublicSearchControllerImpl.class
    })
public class PublicApiExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(PublicApiExceptionHandler.class);

  /** Responde con una clave localizable constante ante cualquier fallo no clasificado. */
  @ExceptionHandler(Exception.class)
  ResponseEntity<PublicApiErrorResponse> handleUnexpected(Exception exception) {
    LOGGER.error("public_api_failure type={}", exception.getClass().getSimpleName());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(PublicApiErrorResponse.unavailable());
  }
}
