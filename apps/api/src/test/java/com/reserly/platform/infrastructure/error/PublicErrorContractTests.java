package com.reserly.platform.infrastructure.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.reserly.platform.availability.dto.AvailabilityErrorResponse;
import com.reserly.platform.billing.dto.BillingErrorResponse;
import com.reserly.platform.identity.dto.AuthenticationErrorResponse;
import com.reserly.platform.identity.dto.EmailVerificationErrorResponse;
import com.reserly.platform.identity.dto.PasswordResetErrorResponse;
import com.reserly.platform.identity.dto.RegistrationErrorResponse;
import com.reserly.platform.infrastructure.ratelimit.RateLimitErrorResponse;
import com.reserly.platform.infrastructure.validation.RequestValidationErrorResponse;
import com.reserly.platform.reservations.dto.ReservationErrorResponse;
import com.reserly.platform.reservations.dto.ReservationRestrictionErrorResponse;
import com.reserly.platform.reviews.dto.ReviewErrorResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifica que todo DTO de error usado por superficies públicas incluya una clave i18n cerrada. */
class PublicErrorContractTests {

  @Test
  void publicErrorDtosExposeMessageKeyWithoutFreeFormDetailFields() {
    List<Class<?>> responseTypes =
        List.of(
            AuthenticationErrorResponse.class,
            EmailVerificationErrorResponse.class,
            PasswordResetErrorResponse.class,
            RegistrationErrorResponse.class,
            ReservationErrorResponse.class,
            ReservationRestrictionErrorResponse.class,
            ReviewErrorResponse.class,
            AvailabilityErrorResponse.class,
            RateLimitErrorResponse.class,
            RequestValidationErrorResponse.class,
            BillingErrorResponse.class,
            PublicApiErrorResponse.class);

    responseTypes.forEach(
        responseType -> {
          List<String> fields =
              Arrays.stream(responseType.getRecordComponents())
                  .map(component -> component.getName())
                  .toList();
          assertThat(fields).contains("messageKey");
          assertThat(fields)
              .doesNotContain("message", "detail", "provider", "providerMessage", "cause");
        });
  }

  @Test
  void catalogProducesOnlyStableTranslationKeys() {
    assertThat(PublicErrorMessageCatalog.messageKey("AUTHENTICATION_INVALID"))
        .isEqualTo("PublicErrors.authenticationInvalid");
    assertThat(PublicErrorMessageCatalog.messageKey("REDSYS_CALLBACK_INVALID"))
        .isEqualTo("PublicErrors.invalidRequest");
    assertThat(PublicErrorMessageCatalog.messageKey("PUBLIC_SERVICE_UNAVAILABLE"))
        .isEqualTo("PublicErrors.unavailable");
  }

  @Test
  void unexpectedFailureResponseDoesNotReflectExternalDetails() {
    String externalDetail = "VIES SOAP response: <taxId>B12345678</taxId>";
    var response =
        new PublicApiExceptionHandler().handleUnexpected(new IllegalStateException(externalDetail));

    assertThat(response.getStatusCode().value()).isEqualTo(500);
    assertThat(response.getBody()).isEqualTo(PublicApiErrorResponse.unavailable());
    assertThat(response.getBody().toString()).doesNotContain(externalDetail, "VIES", "taxId");
  }
}
