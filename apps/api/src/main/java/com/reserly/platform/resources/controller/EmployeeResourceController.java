package com.reserly.platform.resources.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.resources.dto.EmployeeResourceHourResponse;
import com.reserly.platform.resources.dto.EmployeeResourceRequest;
import com.reserly.platform.resources.dto.EmployeeResourceResponse;
import com.reserly.platform.resources.dto.EmployeeResourceWeeklyHoursRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/** Contrato privado para administrar equipo y recursos del local autenticado. */
public interface EmployeeResourceController {

  @GetMapping(path = "/api/venue/me/team")
  ResponseEntity<List<EmployeeResourceResponse>> list(
      @AuthenticationPrincipal AuthenticatedAccount account);

  @PostMapping(path = "/api/venue/me/team")
  ResponseEntity<EmployeeResourceResponse> create(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody EmployeeResourceRequest request);

  @PatchMapping(path = "/api/venue/me/team/{resourceId}")
  ResponseEntity<EmployeeResourceResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID resourceId,
      @Valid @RequestBody EmployeeResourceRequest request);

  @GetMapping(path = "/api/venue/me/team/{resourceId}/weekly-hours")
  ResponseEntity<List<EmployeeResourceHourResponse>> listWeeklyHours(
      @AuthenticationPrincipal AuthenticatedAccount account, @PathVariable UUID resourceId);

  @PutMapping(path = "/api/venue/me/team/{resourceId}/weekly-hours")
  ResponseEntity<List<EmployeeResourceHourResponse>> replaceWeeklyHours(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID resourceId,
      @Valid @RequestBody EmployeeResourceWeeklyHoursRequest request);
}
