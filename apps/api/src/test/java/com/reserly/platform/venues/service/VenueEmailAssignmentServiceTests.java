package com.reserly.platform.venues.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Verifica listado multi-local, normalización y aislamiento de asociaciones operativas. */
@ExtendWith(MockitoExtension.class)
class VenueEmailAssignmentServiceTests {

  @Mock private VenueDao venueDao;

  private VenueEmailAssignmentServiceImpl service;
  private UUID ownerUserId;

  @BeforeEach
  void setUp() {
    service = new VenueEmailAssignmentServiceImpl(venueDao);
    ownerUserId = UUID.randomUUID();
  }

  @Test
  void listsEveryPublishedVenueOwnedByTheAccount() {
    VenueEntity first = venue("Ames", "ames", "ames@reserly.local");
    VenueEntity second = venue("Brisa", "brisa", "brisa@reserly.local");
    when(venueDao.findAllPublishedByOwnerUserId(ownerUserId)).thenReturn(List.of(first, second));

    var result = service.list(ownerUserId);

    assertThat(result).extracting(item -> item.venueName()).containsExactly("Ames", "Brisa");
    assertThat(result)
        .extracting(item -> item.email())
        .containsExactly("ames@reserly.local", "brisa@reserly.local");
  }

  @Test
  void updatesOnlyTheExplicitOwnedVenueAndNormalizesTheEmail() {
    UUID venueId = UUID.randomUUID();
    VenueEntity venue = venue("Ames", "ames", "old@reserly.local");
    venue.setId(venueId);
    when(venueDao.findPublishedOwnedByIdForUpdate(ownerUserId, venueId))
        .thenReturn(Optional.of(venue));
    when(venueDao.saveAndFlush(venue)).thenReturn(venue);

    var result = service.update(ownerUserId, venueId, "  EQUIPO@AMES.LOCAL  ");

    assertThat(result.email()).isEqualTo("equipo@ames.local");
    verify(venueDao).saveAndFlush(venue);
  }

  @Test
  void hidesWhetherAnUnownedOrUnpublishedVenueExists() {
    UUID venueId = UUID.randomUUID();
    when(venueDao.findPublishedOwnedByIdForUpdate(ownerUserId, venueId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.update(ownerUserId, venueId, "email@example.com"))
        .isInstanceOf(VenueProfileNotFoundException.class);
  }

  private VenueEntity venue(String name, String slug, String email) {
    VenueEntity venue = new VenueEntity();
    venue.setId(UUID.randomUUID());
    venue.setName(name);
    venue.setSlug(slug);
    venue.setNotificationEmail(email);
    venue.setUpdatedAt(Instant.parse("2026-08-02T20:00:00Z"));
    return venue;
  }
}
