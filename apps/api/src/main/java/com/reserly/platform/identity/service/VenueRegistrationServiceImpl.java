package com.reserly.platform.identity.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.validation.BusinessTaxIdentifierValidationException;
import com.reserly.platform.businessverification.validation.BusinessTaxIdentifierValidationService;
import com.reserly.platform.businessverification.validation.NormalizedBusinessTaxIdentifier;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.dto.VenueRegistrationCommand;
import com.reserly.platform.identity.dto.VenueRegistrationResponse;
import com.reserly.platform.identity.persistence.RoleDao;
import com.reserly.platform.identity.persistence.RoleEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.persistence.UserRoleDao;
import com.reserly.platform.identity.persistence.UserRoleEntity;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación atómica del alta empresarial.
 *
 * <p>La identidad fiscal se normaliza y valida mediante estrategias nacionales antes de consultar
 * unicidad o escribir datos. Superar esa comprobación local no equivale a una verificación
 * empresarial remota.
 */
@Service
public class VenueRegistrationServiceImpl implements VenueRegistrationService {

  private static final String VENUE_OWNER_ROLE = "venue_owner";
  private static final String INITIAL_USER_STATUS = "pending_email_verification";
  private static final String INITIAL_BUSINESS_STATUS = "unverified";
  private final UserDao userDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final BusinessAccountDao businessAccountDao;
  private final PasswordHashingService passwordHashingService;
  private final BusinessTaxIdentifierValidationService taxIdentifierValidationService;

  public VenueRegistrationServiceImpl(
      UserDao userDao,
      RoleDao roleDao,
      UserRoleDao userRoleDao,
      BusinessAccountDao businessAccountDao,
      PasswordHashingService passwordHashingService,
      BusinessTaxIdentifierValidationService taxIdentifierValidationService) {
    this.userDao = userDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.businessAccountDao = businessAccountDao;
    this.passwordHashingService = passwordHashingService;
    this.taxIdentifierValidationService = taxIdentifierValidationService;
  }

  @Override
  @Transactional
  public VenueRegistrationResponse register(VenueRegistrationCommand command) {
    try {
      passwordHashingService.validate(command.rawPassword());
    } catch (PasswordHashingValidationException exception) {
      throw new RegistrationValidationException();
    }

    String normalizedEmail = normalizeEmail(command.email());
    NormalizedBusinessTaxIdentifier taxIdentifier;
    try {
      taxIdentifier =
          taxIdentifierValidationService.normalizeAndValidate(
              command.taxCountry(), command.businessTaxIdentifier());
    } catch (BusinessTaxIdentifierValidationException exception) {
      throw new RegistrationValidationException();
    }

    if (userDao.existsByEmailNormalized(normalizedEmail)
        || businessAccountDao.existsByTaxIdentity(
            taxIdentifier.taxCountry(), taxIdentifier.value())) {
      throw new RegistrationConflictException();
    }

    try {
      Instant now = Instant.now();
      UserEntity user = createUser(command, normalizedEmail, now);
      userDao.saveAndFlush(user);

      BusinessAccountEntity businessAccount =
          createBusinessAccount(command, user, taxIdentifier, now);
      businessAccountDao.saveAndFlush(businessAccount);

      RoleEntity venueOwnerRole =
          roleDao
              .findByCode(VENUE_OWNER_ROLE)
              .orElseThrow(() -> new IllegalStateException("Required venue owner role is missing"));
      UserRoleEntity assignment = new UserRoleEntity();
      assignment.setUser(user);
      assignment.setRole(venueOwnerRole);
      assignment.setAssignedAt(now);
      userRoleDao.saveAndFlush(assignment);

      return new VenueRegistrationResponse(
          user.getId(),
          businessAccount.getId(),
          AccountType.VENUE_BUSINESS.persistedValue(),
          INITIAL_BUSINESS_STATUS,
          true,
          false);
    } catch (DataIntegrityViolationException exception) {
      // Cubre carreras entre los prechecks y los índices únicos sin filtrar detalles internos.
      throw new RegistrationConflictException(exception);
    }
  }

  private UserEntity createUser(
      VenueRegistrationCommand command, String normalizedEmail, Instant now) {
    UserEntity user = new UserEntity();
    user.setEmail(command.email().strip());
    user.setEmailNormalized(normalizedEmail);
    user.setPasswordHash(passwordHashingService.hash(command.rawPassword()));
    user.setAccountType(AccountType.VENUE_BUSINESS);
    user.setPreferredLocale(command.preferredLocale());
    user.setStatus(INITIAL_USER_STATUS);
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    return user;
  }

  private BusinessAccountEntity createBusinessAccount(
      VenueRegistrationCommand command,
      UserEntity user,
      NormalizedBusinessTaxIdentifier taxIdentifier,
      Instant now) {
    BusinessAccountEntity businessAccount = new BusinessAccountEntity();
    businessAccount.setOwnerUser(user);
    businessAccount.setTaxCountry(taxIdentifier.taxCountry());
    businessAccount.setBusinessLegalName(command.businessLegalName().strip());
    businessAccount.setBusinessTaxIdentifier(command.businessTaxIdentifier().strip());
    businessAccount.setBusinessTaxIdentifierNormalized(taxIdentifier.value());
    businessAccount.setBusinessAddress(normalizeOptional(command.businessAddress()));
    businessAccount.setBusinessVerificationStatus(INITIAL_BUSINESS_STATUS);
    businessAccount.setCreatedAt(now);
    businessAccount.setUpdatedAt(now);
    return businessAccount;
  }

  private String normalizeEmail(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.strip();
  }
}
