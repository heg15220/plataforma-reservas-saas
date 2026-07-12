package com.reserly.platform.services.controller;

import com.reserly.platform.identity.security.AuthenticatedAccount;
import com.reserly.platform.services.dto.ServiceRequest;
import com.reserly.platform.services.dto.ServiceResourceAssignmentRequest;
import com.reserly.platform.services.dto.ServiceResponse;
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

/** Contrato privado para administrar los servicios del local autenticado. */
public interface ServiceController {

  @GetMapping(path = "/api/venue/me/services")
  ResponseEntity<List<ServiceResponse>> list(@AuthenticationPrincipal AuthenticatedAccount account);

  @PostMapping(path = "/api/venue/me/services")
  ResponseEntity<ServiceResponse> create(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @Valid @RequestBody ServiceRequest request);

  @PatchMapping(path = "/api/venue/me/services/{serviceId}")
  ResponseEntity<ServiceResponse> update(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID serviceId,
      @Valid @RequestBody ServiceRequest request);

  @PutMapping(path = "/api/venue/me/services/{serviceId}/resources")
  ResponseEntity<ServiceResponse> replaceCompatibleResources(
      @AuthenticationPrincipal AuthenticatedAccount account,
      @PathVariable UUID serviceId,
      @Valid @RequestBody ServiceResourceAssignmentRequest request);
}
