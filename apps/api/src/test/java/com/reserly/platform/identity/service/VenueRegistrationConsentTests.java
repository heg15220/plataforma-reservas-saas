package com.reserly.platform.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.validation.BusinessTaxIdentifierScheme;
import com.reserly.platform.businessverification.validation.BusinessTaxIdentifierValidationService;
import com.reserly.platform.businessverification.validation.NormalizedBusinessTaxIdentifier;
import com.reserly.platform.identity.dto.VenueRegistrationCommand;
import com.reserly.platform.identity.persistence.RoleDao;
import com.reserly.platform.identity.persistence.RoleEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.persistence.UserRoleDao;
import com.reserly.platform.infrastructure.legal.LegalDocumentVersions;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica que el alta exige y conserva evidencia mínima de aceptación legal. */
@ExtendWith(MockitoExtension.class)
class VenueRegistrationConsentTests {

  @Mock private UserDao userDao;
  @Mock private RoleDao roleDao;
  @Mock private UserRoleDao userRoleDao;
  @Mock private BusinessAccountDao businessAccountDao;
  @Mock private PasswordHashingService passwordHashingService;
  @Mock private BusinessTaxIdentifierValidationService taxIdentifierValidationService;
  @Mock private EmailVerificationService emailVerificationService;

  @Test
  void rejectsMissingConsentBeforeHashingOrPersistence() {
    VenueRegistrationServiceImpl service = service();

    assertThatThrownBy(() -> service.register(command(false)))
        .isInstanceOf(RegistrationValidationException.class);

    verify(passwordHashingService, never()).hash(any());
    verify(userDao, never()).saveAndFlush(any());
  }

  @Test
  void persistsTimestampAndExactVersionsForAcceptedDocuments() {
    VenueRegistrationServiceImpl service = service();
    RoleEntity role = new RoleEntity();
    when(taxIdentifierValidationService.normalizeAndValidate("ES", "B12345674"))
        .thenReturn(
            new NormalizedBusinessTaxIdentifier(
                "ES", "B12345674", BusinessTaxIdentifierScheme.SPAIN_ENTITY_NIF, true, true));
    when(passwordHashingService.hash("correct-horse-battery")).thenReturn("password-hash");
    when(roleDao.findByCode("venue_owner")).thenReturn(Optional.of(role));

    service.register(command(true));

    ArgumentCaptor<UserEntity> user = ArgumentCaptor.forClass(UserEntity.class);
    verify(userDao).saveAndFlush(user.capture());
    assertThat(user.getValue().getLegalTermsAcceptedAt()).isNotNull();
    assertThat(user.getValue().getLegalTermsVersion())
        .isEqualTo(LegalDocumentVersions.TERMS_OF_SERVICE);
    assertThat(user.getValue().getPrivacyPolicyAcceptedAt())
        .isEqualTo(user.getValue().getLegalTermsAcceptedAt());
    assertThat(user.getValue().getPrivacyPolicyVersion())
        .isEqualTo(LegalDocumentVersions.PRIVACY_POLICY);
  }

  private VenueRegistrationServiceImpl service() {
    return new VenueRegistrationServiceImpl(
        userDao,
        roleDao,
        userRoleDao,
        businessAccountDao,
        passwordHashingService,
        taxIdentifierValidationService,
        emailVerificationService);
  }

  private VenueRegistrationCommand command(boolean acceptsLegalTerms) {
    return new VenueRegistrationCommand(
        "local@example.com",
        "correct-horse-battery",
        "es",
        "ES",
        "Empresa SL",
        "B12345674",
        null,
        acceptsLegalTerms);
  }
}
