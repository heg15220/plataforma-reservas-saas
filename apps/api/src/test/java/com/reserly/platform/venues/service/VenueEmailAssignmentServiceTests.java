package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.identity.persistence.AuthSessionDao;
import com.reserly.platform.identity.persistence.RoleDao;
import com.reserly.platform.identity.persistence.RoleEntity;
import com.reserly.platform.identity.persistence.UserDao;
import com.reserly.platform.identity.persistence.UserEntity;
import com.reserly.platform.identity.persistence.UserRoleDao;
import com.reserly.platform.identity.service.PasswordHashingService;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import com.reserly.platform.venues.persistence.VenuePanelCredentialDao;
import com.reserly.platform.venues.persistence.VenuePanelCredentialEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica creación, rotación, normalización y aislamiento de accesos privados por local. */
@ExtendWith(MockitoExtension.class)
class VenueEmailAssignmentServiceTests {

  @Mock private VenueDao venueDao;
  @Mock private VenuePanelCredentialDao credentialDao;
  @Mock private UserDao userDao;
  @Mock private RoleDao roleDao;
  @Mock private UserRoleDao userRoleDao;
  @Mock private AuthSessionDao authSessionDao;
  @Mock private PasswordHashingService passwordHashingService;

  private VenueEmailAssignmentServiceImpl service;
  private UUID ownerUserId;

  @BeforeEach
  void setUp() {
    service =
        new VenueEmailAssignmentServiceImpl(
            venueDao,
            credentialDao,
            userDao,
            roleDao,
            userRoleDao,
            authSessionDao,
            passwordHashingService);
    ownerUserId = UUID.randomUUID();
  }

  @Test
  void listsWhetherEachPublishedVenueAlreadyHasIndividualPanelAccess() {
    VenueEntity first = venue("Ames", "ames", "ames@reserly.local");
    VenueEntity second = venue("Brisa", "brisa", "brisa@reserly.local");
    UserEntity delegated = user(UUID.randomUUID(), "ames@reserly.local");
    VenuePanelCredentialEntity credential = credential(first, delegated);
    when(venueDao.findAllPublishedByOwnerUserId(ownerUserId)).thenReturn(List.of(first, second));
    when(credentialDao.findByVenueId(first.getId())).thenReturn(Optional.of(credential));
    when(credentialDao.findByVenueId(second.getId())).thenReturn(Optional.empty());

    var result = service.list(ownerUserId);

    assertThat(result).extracting(item -> item.venueName()).containsExactly("Ames", "Brisa");
    assertThat(result)
        .extracting(item -> item.panelAccessConfigured())
        .containsExactly(true, false);
  }

  @Test
  void createsAHashedCredentialScopedToTheExplicitOwnedVenue() {
    UUID venueId = UUID.randomUUID();
    VenueEntity venue = venue("Ames", "ames", "old@reserly.local");
    venue.setId(venueId);
    RoleEntity role = new RoleEntity();
    role.setCode("venue_owner");
    when(venueDao.findPublishedOwnedByIdForUpdate(ownerUserId, venueId))
        .thenReturn(Optional.of(venue));
    when(credentialDao.findByVenueIdForUpdate(venueId)).thenReturn(Optional.empty());
    when(userDao.existsByEmailNormalized("equipo@ames.local")).thenReturn(false);
    when(passwordHashingService.hash("UnaClaveSegura2026!")).thenReturn("bcrypt-hash");
    when(roleDao.findByCode("venue_owner")).thenReturn(Optional.of(role));
    when(userDao.getReferenceById(ownerUserId)).thenReturn(new UserEntity());
    when(venueDao.saveAndFlush(venue)).thenReturn(venue);
    when(credentialDao.saveAndFlush(any(VenuePanelCredentialEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result =
        service.update(ownerUserId, venueId, "  EQUIPO@AMES.LOCAL  ", "UnaClaveSegura2026!");

    assertThat(result.email()).isEqualTo("equipo@ames.local");
    assertThat(result.panelAccessConfigured()).isTrue();
    assertThat(venue.getNotificationEmail()).isEqualTo("equipo@ames.local");
    verify(passwordHashingService).validate("UnaClaveSegura2026!");
    verify(passwordHashingService).hash("UnaClaveSegura2026!");
    verify(userRoleDao).saveAndFlush(any());
  }

  @Test
  void rotatesPasswordAndRevokesPreviousSessions() {
    UUID venueId = UUID.randomUUID();
    UUID delegatedUserId = UUID.randomUUID();
    VenueEntity venue = venue("Ames", "ames", "equipo@ames.local");
    venue.setId(venueId);
    UserEntity delegated = user(delegatedUserId, "equipo@ames.local");
    VenuePanelCredentialEntity credential = credential(venue, delegated);
    when(venueDao.findPublishedOwnedByIdForUpdate(ownerUserId, venueId))
        .thenReturn(Optional.of(venue));
    when(credentialDao.findByVenueIdForUpdate(venueId)).thenReturn(Optional.of(credential));
    when(passwordHashingService.hash("OtraClaveSegura2026!")).thenReturn("new-hash");
    when(venueDao.saveAndFlush(venue)).thenReturn(venue);
    when(credentialDao.saveAndFlush(credential)).thenReturn(credential);

    service.update(ownerUserId, venueId, "equipo@ames.local", "OtraClaveSegura2026!");

    assertThat(delegated.getPasswordHash()).isEqualTo("new-hash");
    verify(authSessionDao).revokeActiveByUserId(any(UUID.class), any(Instant.class));
  }

  @Test
  void hidesWhetherAnUnownedOrUnpublishedVenueExists() {
    UUID venueId = UUID.randomUUID();
    when(venueDao.findPublishedOwnedByIdForUpdate(ownerUserId, venueId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(ownerUserId, venueId, "email@example.com", "UnaClaveSegura2026!"))
        .isInstanceOf(VenueProfileNotFoundException.class);
  }

  private VenueEntity venue(String name, String slug, String email) {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setName(name);
    venue.setSlug(slug);
    venue.setDefaultLocale("es");
    venue.setNotificationEmail(email);
    venue.setUpdatedAt(Instant.parse("2026-08-02T20:00:00Z"));
    return venue;
  }

  private UserEntity user(UUID id, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setEmail(email);
    user.setEmailNormalized(email);
    user.setPasswordHash("old-hash");
    return user;
  }

  private VenuePanelCredentialEntity credential(VenueEntity venue, UserEntity user) {
    VenuePanelCredentialEntity credential = new VenuePanelCredentialEntity();
    credential.setVenue(venue);
    credential.setUser(user);
    credential.setCreatedAt(Instant.parse("2026-08-02T20:00:00Z"));
    credential.setUpdatedAt(Instant.parse("2026-08-02T20:00:00Z"));
    return credential;
  }
}
