package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminVenueListResponse;
import com.reserly.platform.administration.dto.AdminVenueResponse;
import com.reserly.platform.administration.dto.AdminVenueSuspensionRequest;
import com.reserly.platform.administration.dto.AdminVenueUpdateRequest;
import com.reserly.platform.venues.persistence.CategoryDao;
import com.reserly.platform.venues.persistence.CategoryEntity;
import com.reserly.platform.venues.persistence.VenueDao;
import com.reserly.platform.venues.persistence.VenueEntity;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Edita solo identidad comercial y contacto; suspensión, propiedad y publicación quedan fuera.
 */
@Service
public class AdminVenueServiceImpl implements AdminVenueService {

  static final int LIST_LIMIT = 100;

  private final VenueDao venueDao;
  private final CategoryDao categoryDao;
  private final AuditLogService auditLogService;
  private final Clock clock;

  public AdminVenueServiceImpl(
      VenueDao venueDao,
      CategoryDao categoryDao,
      AuditLogService auditLogService,
      Clock clock) {
    this.venueDao = venueDao;
    this.categoryDao = categoryDao;
    this.auditLogService = auditLogService;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public AdminVenueListResponse list() {
    return new AdminVenueListResponse(
        venueDao.findAdminPage(PageRequest.of(0, LIST_LIMIT)).stream()
            .map(this::response)
            .toList());
  }

  @Override
  @Transactional
  public AdminVenueResponse update(
      UUID actorUserId,
      UUID venueId,
      AdminVenueUpdateRequest request,
      AdminRequestContext context) {
    VenueEntity venue =
        venueDao
            .findByIdForAdminUpdate(venueId)
            .orElseThrow(AdminResourceNotFoundException::new);
    CategoryEntity category =
        categoryDao
            .findActiveById(request.categoryId())
            .orElseThrow(AdminResourceConflictException::new);
    Map<String, Object> before = snapshot(venue);
    venue.setName(request.name().strip());
    venue.setCategory(category);
    venue.setContactEmail(optional(request.contactEmail()));
    venue.setPhone(optional(request.phone()));
    venue.setAddress(optional(request.address()));
    venue.setCity(optional(request.city()));
    venue.setProvince(optional(request.province()));
    venue.setCountry(optional(request.country()));
    venue.setPostalCode(optional(request.postalCode()));
    venue.setUpdatedAt(clock.instant());
    venueDao.saveAndFlush(venue);
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "venue",
            venue.getId(),
            "venue.basic_details_updated",
            before,
            snapshot(venue),
            context.ipAddress(),
            context.userAgent()));
    return response(venue);
  }

  /**
   * Retira el local de todos los flujos públicos que exigen estado {@code published}.
   *
   * <p>Las reservas existentes y la cuenta propietaria permanecen intactas. El motivo solo se
   * conserva en auditoría para no exponerlo accidentalmente en el perfil.
   */
  @Override
  @Transactional
  public AdminVenueResponse suspend(
      UUID actorUserId,
      UUID venueId,
      AdminVenueSuspensionRequest request,
      AdminRequestContext context) {
    VenueEntity venue =
        venueDao
            .findByIdForAdminUpdate(venueId)
            .orElseThrow(AdminResourceNotFoundException::new);
    if ("suspended".equals(venue.getStatus()) || "archived".equals(venue.getStatus())) {
      throw new AdminResourceConflictException();
    }
    Map<String, Object> before = snapshot(venue);
    venue.setStatus("suspended");
    venue.setUpdatedAt(clock.instant());
    venueDao.saveAndFlush(venue);
    Map<String, Object> after = new java.util.LinkedHashMap<>(snapshot(venue));
    after.put("reason", request.reason().strip());
    auditLogService.record(
        new AuditLogEntry(
            actorUserId,
            "admin",
            "venue",
            venue.getId(),
            "venue.suspended",
            before,
            after,
            context.ipAddress(),
            context.userAgent()));
    return response(venue);
  }

  private AdminVenueResponse response(VenueEntity venue) {
    return new AdminVenueResponse(
        venue.getId(),
        venue.getName(),
        venue.getSlug(),
        venue.getCategory().getId(),
        venue.getCategory().getName(),
        venue.getStatus(),
        venue.getContactEmail(),
        venue.getPhone(),
        venue.getAddress(),
        venue.getCity(),
        venue.getProvince(),
        venue.getCountry(),
        venue.getPostalCode(),
        venue.getUpdatedAt());
  }

  private Map<String, Object> snapshot(VenueEntity venue) {
    return Map.of(
        "name", venue.getName(),
        "categoryId", venue.getCategory().getId().toString(),
        "status", venue.getStatus());
  }

  private String optional(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }
}
