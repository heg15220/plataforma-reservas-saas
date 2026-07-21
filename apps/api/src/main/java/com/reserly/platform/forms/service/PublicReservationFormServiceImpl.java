package com.reserly.platform.forms.service;

import com.reserly.platform.forms.ReservationBaseFieldCatalog;
import com.reserly.platform.forms.converter.ReservationFormFieldConverter;
import com.reserly.platform.forms.dto.PublicReservationFormResponse;
import com.reserly.platform.forms.dto.ReservationFormPreviewFieldResponse;
import com.reserly.platform.forms.persistence.ReservationFormFieldDao;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.service.VenueProfileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ensambla campos base y personalizados solo después de comprobar ambas publicaciones. */
@Service
public class PublicReservationFormServiceImpl implements PublicReservationFormService {
  private final VenueDao venueDao;
  private final ReservationFormFieldDao fieldDao;
  private final ReservationFormFieldConverter converter;

  public PublicReservationFormServiceImpl(
      VenueDao venueDao,
      ReservationFormFieldDao fieldDao,
      ReservationFormFieldConverter converter) {
    this.venueDao = venueDao;
    this.fieldDao = fieldDao;
    this.converter = converter;
  }

  @Override
  @Transactional(readOnly = true)
  public PublicReservationFormResponse findPublishedByVenueSlug(String venueSlug) {
    var venue =
        venueDao
            .findPublishedBySlug(venueSlug)
            .orElseThrow(VenueProfileNotFoundException::new);
    if (!venue.isReservationFormPublished()) {
      throw new VenueProfileNotFoundException();
    }
    var base = ReservationBaseFieldCatalog.fields();
    List<ReservationFormPreviewFieldResponse> fields = new ArrayList<>();
    base.forEach(
        field ->
            fields.add(
                new ReservationFormPreviewFieldResponse(
                    null,
                    "base",
                    field.key(),
                    field.inputType(),
                    null,
                    field.labelKey(),
                    null,
                    field.required(),
                    false,
                    null,
                    null,
                    field.position())));
    var custom = fieldDao.findAllPublishedByVenue(venue.getId());
    for (int index = 0; index < custom.size(); index++) {
      var field = custom.get(index);
      fields.add(
          new ReservationFormPreviewFieldResponse(
              field.getId(),
              "custom",
              field.getKey(),
              field.getType().code(),
              field.getLabel(),
              null,
              converter.toDto(field.getLabelI18n()),
              field.isRequired(),
              false,
              field.getOptions(),
              converter.toDtos(field.getOptionsI18n()),
              base.size() + index));
    }
    return new PublicReservationFormResponse(venue.getId(), venue.getSlug(), fields);
  }
}