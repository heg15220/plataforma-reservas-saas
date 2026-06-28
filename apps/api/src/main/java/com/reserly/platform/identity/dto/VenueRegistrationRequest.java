package com.reserly.platform.identity.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud pública de alta de una cuenta empresarial.
 *
 * <p>No admite elegir {@code accountType}, rol ni estado de verificación. Esos valores los fija el
 * backend. Los datos del perfil de local se incorporarán cuando exista el modelo de locales en la
 * Fase 2.
 *
 * @param account credenciales y preferencia de idioma
 * @param business identidad fiscal mínima
 * @param acceptsLegalTerms aceptación explícita de condiciones
 */
public record VenueRegistrationRequest(
    @NotNull @Valid AccountRegistrationDto account,
    @NotNull @Valid BusinessRegistrationDto business,
    @AssertTrue boolean acceptsLegalTerms) {

  /**
   * Datos de cuenta aceptados por el registro.
   *
   * @param email identidad de acceso
   * @param password secreto de 12 a 72 caracteres, además limitado a 72 bytes por servicio
   * @param preferredLocale locale exacto {@code es} o {@code en}
   */
  public record AccountRegistrationDto(
      @NotBlank @Email @Size(max = 320) String email,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotBlank @Pattern(regexp = "^(es|en)$") String preferredLocale) {}

  /**
   * Identidad empresarial mínima.
   *
   * @param taxCountry país fiscal ISO alpha-2
   * @param legalName razón social
   * @param taxIdentifier identificador fiscal o registral aportado
   * @param registeredAddress dirección empresarial opcional
   */
  public record BusinessRegistrationDto(
      @NotBlank @Pattern(regexp = "^[A-Za-z]{2}$") String taxCountry,
      @NotBlank @Size(max = 255) String legalName,
      @NotBlank @Size(max = 64) String taxIdentifier,
      @Size(max = 500) String registeredAddress) {}
}
