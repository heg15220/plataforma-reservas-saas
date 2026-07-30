package com.reserly.platform.administration.dto;

import java.util.List;

/** Cola administrativa acotada de documentos pendientes. */
public record AdminDocumentListResponse(List<AdminDocumentResponse> documents) {}
