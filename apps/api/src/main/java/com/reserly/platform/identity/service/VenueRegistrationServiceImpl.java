package com.reserly.platform.identity.service;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.dto.VenueRegistrationCommand;
import com.reserly.platform.identity.dto.VenueRegistrationResponse;
import com.reserly.platform.identity.persistence.RoleDao;
import com.reserly.platform.identity.persistence.RoleEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.persistence.UserRoleDao;
import com.reserly.platform.identity.persistence.UserRoleEntity;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación atómica del alta empresarial.
 *
 * <p>La normalización fiscal aplicada aquí es provisional y conservadora: trim y mayúsculas. La
 * tarea 1.5 incorporará reglas por país, eliminación segura de separadores y dígitos de control.
 */
@Service
public class VenueRegistrationServiceImpl implements VenueRegistrationService {

  private static final String VENUE_OWNER_ROLE = "venue_owner";
  private static final String INITIAL_USER_STATUS = "pending_email_verification";
  private static final String INITIAL_BUSINESS_STATUS = "unverified";
  private static final int BCRYPT_MAX_PASSWORD_BYTES = 72;

  private final UserDao userDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final BusinessAccountDao businessAccountDao;
  private final PasswordHashingService passwordHashingService;

  public VenueRegistrationServiceImpl(
      UserDao userDao,
      RoleDao roleDao,
      UserRoleDao userRoleDao,
      BusinessAccountDao businessAccountDao,
      PasswordHashingService passwordHashingService) {
    this.userDao = userDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.businessAccountDao = businessAccountDao;
    this.passwordHashingService = passwordHashingService;
  }

  @Override
  @Transactional
  public VenueRegistrationResponse register(VenueRegistrationCommand command) {
    validatePasswordBytes(command.rawPassword());

    String normalizedEmail = normalizeEmail(command.email());
    String taxCountry = command.taxCountry().strip().toUpperCase(Locale.ROOT);
    String normalizedTaxIdentifier =
        command.businessTaxIdentifier().strip().toUpperCase(Locale.ROOT);

    if (userDao.existsByEmailNormalized(normalizedEmail)
        || businessAccountDao.existsByTaxIdentity(taxCountry, normalizedTaxIdentifier)) {
      throw new RegistrationConflictException();
    }

    try {
      Instant now = Instant.now();
      UserEntity user = createUser(command, normalizedEmail, now);
      userDao.saveAndFlush(user);

      BusinessAccountEntity businessAccount =
          createBusinessAccount(command, user, taxCountry, normalizedTaxIdentifier, now);
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
      String taxCountry,
      String normalizedTaxIdentifier,
      Instant now) {
    BusinessAccountEntity businessAccount = new BusinessAccountEntity();
    businessAccount.setOwnerUser(user);
    businessAccount.setTaxCountry(taxCountry);
    businessAccount.setBusinessLegalName(command.businessLegalName().strip());
    businessAccount.setBusinessTaxIdentifier(command.businessTaxIdentifier().strip());
    businessAccount.setBusinessTaxIdentifierNormalized(normalizedTaxIdentifier);
    businessAccount.setBusinessAddress(normalizeOptional(command.businessAddress()));
    businessAccount.setBusinessVerificationStatus(INITIAL_BUSINESS_STATUS);
    businessAccount.setCreatedAt(now);
    businessAccount.setUpdatedAt(now);
    return businessAccount;
  }

  private void validatePasswordBytes(String rawPassword) {
    if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_PASSWORD_BYTES) {
      throw new RegistrationValidationException();
    }
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
