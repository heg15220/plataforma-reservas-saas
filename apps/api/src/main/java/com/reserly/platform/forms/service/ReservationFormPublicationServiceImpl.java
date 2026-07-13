package com.reserly.platform.forms.service;

import com.reserly.platform.forms.dto.ReservationFormPublicationResponse;
import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.forms.persistence.ReservationFormFieldEntity;
import com.reserly.platform.forms.persistence.ReservationFormFieldType;
import com.reserly.platform.localization.LocalizedText;
import com.reserly.platform.localization.SupportedLocale;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Publicaci?n transaccional que exige ES/EN completos o consentimiento de fallback. */
@Service
public class ReservationFormPublicationServiceImpl implements ReservationFormPublicationService {
  private static final Set<SupportedLocale> REQUIRED_LOCALES =
      Set.of(SupportedLocale.ES, SupportedLocale.EN);

  private final VenueDao venueDao;
  private final ReservationFormFieldDao fieldDao;

  public ReservationFormPublicationServiceImpl(VenueDao venueDao, ReservationFormFieldDao fieldDao) {
    this.venueDao = venueDao;
    this.fieldDao = fieldDao;
  }

  @Override
  @Transactional(readOnly = true)
  public ReservationFormPublicationResponse status(UUID ownerUserId) {
    VenueEntity venue =
        venueDao
            .findCurrentByOwnerUserId(ownerUserId)
            .orElseThrow(ReservationFormFieldNotFoundException::new);
    return response(venue, missingTranslations(fieldDao.findAllOwned(ownerUserId)));
  }

  @Override
  @Transactional
  public ReservationFormPublicationResponse update(
      UUID ownerUserId, boolean published, boolean fallbackApproved) {
    VenueEntity venue =
        venueDao
            .findCurrentByOwnerUserIdForUpdate(ownerUserId)
            .orElseThrow(ReservationFormFieldNotFoundException::new);
    List<ReservationFormFieldEntity> fields = fieldDao.findAllOwnedForUpdate(ownerUserId);
    List<String> missing = missingTranslations(fields);
    if (published && !missing.isEmpty() && !fallbackApproved) {
      throw new ReservationFormPublicationInvalidException();
    }

    venue.setReservationFormPublished(published);
    venue.setReservationFormFallbackApproved(published && fallbackApproved);
    venue.setReservationFormPublishedAt(published ? Instant.now() : null);
    venueDao.saveAndFlush(venue);
    return response(venue, missing);
  }

  private ReservationFormPublicationResponse response(VenueEntity venue, List<String> missing) {
    return new ReservationFormPublicationResponse(
        venue.isReservationFormPublished(),
        venue.isReservationFormFallbackApproved(),
        missing.isEmpty(),
        missing,
        venue.getReservationFormPublishedAt());
  }

  private List<String> missingTranslations(List<ReservationFormFieldEntity> fields) {
    List<String> missing = new ArrayList<>();
    for (ReservationFormFieldEntity field : fields) {
      addMissing(missing, field.getKey() + ".label", field.getLabelI18n());
      if (field.getType() == ReservationFormFieldType.SELECT) {
        List<LocalizedText> localizedOptions = field.getOptionsI18n();
        int optionCount = field.getOptions() == null ? 0 : field.getOptions().size();
        for (int index = 0; index < optionCount; index++) {
          LocalizedText option =
              localizedOptions != null && index < localizedOptions.size()
                  ? localizedOptions.get(index)
                  : null;
          addMissing(missing, field.getKey() + ".options." + index, option);
        }
      }
    }
    return List.copyOf(missing);
  }

  private void addMissing(List<String> missing, String path, LocalizedText text) {
    if (text == null) {
      missing.add(path + ".es");
      missing.add(path + ".en");
      return;
    }
    text.missingTranslations(REQUIRED_LOCALES)
        .stream()
        .map(locale -> path + "." + locale.languageTag())
        .sorted()
        .forEach(missing::add);
  }
}
