package com.reserly.platform.venues.service;

import com.reserly.platform.identity.AccountType;
import com.reserly.platform.identity.persistence.AuthSessionDao;
import com.reserly.platform.identity.persistence.RoleDao;
import com.reserly.platform.identity.persistence.RoleEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.persistence.UserRoleDao;
import com.reserly.platform.identity.persistence.UserRoleEntity;
import com.reserly.platform.identity.service.PasswordHashingService;
import com.reserly.platform.identity.service.PasswordHashingValidationException;
import com.reserly.platform.venues.dto.VenueEmailAssignmentResponse;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.persistence.VenuePanelCredentialDao;
import com.reserly.platform.venues.persistence.VenuePanelCredentialEntity;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación transaccional de destinatarios y accesos privados por local.
 *
 * <p>La cuenta empresarial conserva la propiedad de todas sus sedes. Cada credencial creada queda
 * vinculada a un único local y cualquier rotación de contraseña revoca sus sesiones anteriores.
 */
@Service
public class VenueEmailAssignmentServiceImpl implements VenueEmailAssignmentService {

  private static final String VENUE_OWNER_ROLE = "venue_owner";
  private final VenueDao venueDao;
  private final VenuePanelCredentialDao credentialDao;
  private final UserDao userDao;
  private final RoleDao roleDao;
  private final UserRoleDao userRoleDao;
  private final AuthSessionDao authSessionDao;
  private final PasswordHashingService passwordHashingService;

  public VenueEmailAssignmentServiceImpl(
      VenueDao venueDao,
      VenuePanelCredentialDao credentialDao,
      UserDao userDao,
      RoleDao roleDao,
      UserRoleDao userRoleDao,
      AuthSessionDao authSessionDao,
      PasswordHashingService passwordHashingService) {
    this.venueDao = venueDao;
    this.credentialDao = credentialDao;
    this.userDao = userDao;
    this.roleDao = roleDao;
    this.userRoleDao = userRoleDao;
    this.authSessionDao = authSessionDao;
    this.passwordHashingService = passwordHashingService;
  }

  @Override
  @Transactional(readOnly = true)
  public List<VenueEmailAssignmentResponse> list(UUID ownerUserId) {
    return venueDao.findAllPublishedByOwnerUserId(ownerUserId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public VenueEmailAssignmentResponse update(
      UUID ownerUserId, UUID venueId, String email, String rawPassword) {
    validatePassword(rawPassword);
    VenueEntity venue =
        venueDao
            .findPublishedOwnedByIdForUpdate(ownerUserId, venueId)
            .orElseThrow(VenueProfileNotFoundException::new);
    String normalizedEmail = normalize(email);
    Instant now = Instant.now();

    try {
      VenuePanelCredentialEntity credential =
          credentialDao
              .findByVenueIdForUpdate(venueId)
              .map(
                  existing -> {
                    rotateUser(existing.getUser(), normalizedEmail, rawPassword, now);
                    existing.setUpdatedAt(now);
                    return existing;
                  })
              .orElseGet(
                  () -> createCredential(ownerUserId, venue, normalizedEmail, rawPassword, now));

      venue.setNotificationEmail(normalizedEmail);
      venue.setUpdatedAt(now);
      venueDao.saveAndFlush(venue);
      credentialDao.saveAndFlush(credential);
      return toResponse(venue, credential);
    } catch (DataIntegrityViolationException exception) {
      // Los índices únicos resuelven carreras sin revelar qué cuenta utiliza el email.
      throw new VenueProfileConflictException(exception);
    }
  }

  private VenuePanelCredentialEntity createCredential(
      UUID ownerUserId,
      VenueEntity venue,
      String normalizedEmail,
      String rawPassword,
      Instant now) {
    if (userDao.existsByEmailNormalized(normalizedEmail)) {
      throw new VenueProfileConflictException();
    }

    UserEntity user = new UserEntity();
    user.setEmail(normalizedEmail);
    user.setEmailNormalized(normalizedEmail);
    user.setPasswordHash(passwordHashingService.hash(rawPassword));
    user.setAccountType(AccountType.VENUE_BUSINESS);
    user.setPreferredLocale(venue.getDefaultLocale());
    user.setEmailVerifiedAt(now);
    user.setStatus("active");
    user.setCreatedAt(now);
    user.setUpdatedAt(now);
    userDao.saveAndFlush(user);

    RoleEntity role =
        roleDao
            .findByCode(VENUE_OWNER_ROLE)
            .orElseThrow(() -> new IllegalStateException("Required venue owner role is missing"));
    UserRoleEntity roleAssignment = new UserRoleEntity();
    roleAssignment.setUser(user);
    roleAssignment.setRole(role);
    roleAssignment.setAssignedByUser(userDao.getReferenceById(ownerUserId));
    roleAssignment.setAssignedAt(now);
    userRoleDao.saveAndFlush(roleAssignment);

    VenuePanelCredentialEntity credential = new VenuePanelCredentialEntity();
    credential.setVenue(venue);
    credential.setUser(user);
    credential.setCreatedAt(now);
    credential.setUpdatedAt(now);
    return credential;
  }

  private void rotateUser(
      UserEntity user, String normalizedEmail, String rawPassword, Instant now) {
    if (!user.getEmailNormalized().equals(normalizedEmail)
        && userDao.existsByEmailNormalized(normalizedEmail)) {
      throw new VenueProfileConflictException();
    }
    user.setEmail(normalizedEmail);
    user.setEmailNormalized(normalizedEmail);
    user.setPasswordHash(passwordHashingService.hash(rawPassword));
    user.setUpdatedAt(now);
    userDao.saveAndFlush(user);
    authSessionDao.revokeActiveByUserId(user.getId(), now);
  }

  private void validatePassword(String rawPassword) {
    try {
      passwordHashingService.validate(rawPassword);
    } catch (PasswordHashingValidationException exception) {
      throw new VenueProfileInvalidException();
    }
  }

  private String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  private VenueEmailAssignmentResponse toResponse(VenueEntity venue) {
    return credentialDao
        .findByVenueId(venue.getId())
        .map(credential -> toResponse(venue, credential))
        .orElseGet(
            () ->
                new VenueEmailAssignmentResponse(
                    venue.getId(),
                    venue.getName(),
                    venue.getSlug(),
                    venue.getNotificationEmail(),
                    false,
                    venue.getUpdatedAt()));
  }

  private VenueEmailAssignmentResponse toResponse(
      VenueEntity venue, VenuePanelCredentialEntity credential) {
    return new VenueEmailAssignmentResponse(
        venue.getId(),
        venue.getName(),
        venue.getSlug(),
        credential.getUser().getEmail(),
        true,
        venue.getUpdatedAt());
  }
}
