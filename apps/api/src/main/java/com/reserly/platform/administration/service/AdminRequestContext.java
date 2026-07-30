package com.reserly.platform.administration.service;

/** Metadatos de red observados para auditoría, nunca confiados desde el JSON. */
public record AdminRequestContext(String ipAddress, String userAgent) {}
