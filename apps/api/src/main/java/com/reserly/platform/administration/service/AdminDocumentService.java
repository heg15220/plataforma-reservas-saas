package com.reserly.platform.administration.service;

import com.reserly.platform.administration.dto.AdminDocumentListResponse;
import com.reserly.platform.administration.dto.AdminDocumentResponse;
import com.reserly.platform.administration.dto.AdminDocumentReviewRequest;
import java.util.UUID;

/** Cola y decisiones administrativas sobre documentos empresariales privados. */
public interface AdminDocumentService {
  AdminDocumentListResponse listPending();

  AdminDocumentContent content(UUID documentId);

  AdminDocumentResponse review(
      UUID actorUserId,
      UUID documentId,
      AdminDocumentReviewRequest request,
      AdminRequestContext context);
}
