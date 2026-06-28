package com.reserly.platform.businessverification.matching;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica tolerancia Unicode, umbrales y tratamiento explícito de datos no publicados. */
class BusinessIdentityMatchingServiceTests {

  private BusinessIdentityMatchingService service;

  @BeforeEach
  void createService() {
    service =
        new BusinessIdentityMatchingServiceImpl(new BusinessIdentityMatchingProperties(0.85, 0.75));
  }

  @Test
  void matchesLegalNameIgnoringCaseDiacriticsAndPresentationPunctuation() {
    assertThat(service.matchesLegalName("Órbita Servicios, S.L.", "ORBITA SERVICIOS SL")).isTrue();
  }

  @Test
  void rejectsMateriallyDifferentLegalName() {
    assertThat(service.matchesLegalName("Órbita Servicios SL", "Atlántico Deportes SA")).isFalse();
  }

  @Test
  void appliesMoreTolerantAddressThreshold() {
    assertThat(service.matchesAddress("Calle Alcalá 10, Madrid", "CALLE ALCALA, 10 28014 MADRID"))
        .isTrue();
  }

  @Test
  void returnsUnknownWhenProviderOmitsValue() {
    assertThat(service.matchesLegalName("Empresa SL", "---")).isNull();
    assertThat(service.matchesAddress("Calle Ejemplo 1", null)).isNull();
  }
}
