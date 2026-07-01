import type { StatusTone } from "@/components/visual";

export const businessVerificationStatuses = [
  "unverified",
  "pending_remote_check",
  "verified",
  "pending_review",
  "rejected",
  "expired",
] as const;

export const emailVerificationStatuses = ["pending", "verified"] as const;

export const manualReviewStatuses = [
  "pending_review",
  "approved",
  "rejected",
  "needs_correction",
] as const;

export const documentReviewStatuses = [
  "pending_review",
  "accepted",
  "rejected",
  "needs_correction",
] as const;

export type BusinessVerificationStatus = (typeof businessVerificationStatuses)[number];
export type EmailVerificationStatus = (typeof emailVerificationStatuses)[number];
export type ManualReviewStatus = (typeof manualReviewStatuses)[number];
export type DocumentReviewStatus = (typeof documentReviewStatuses)[number];

export interface VerificationStatusPresentation {
  titleKey: string;
  descriptionKey: string;
  tone: StatusTone;
}

/**
 * Contrato exhaustivo entre estados persistidos y textos presentables.
 *
 * Ninguna pantalla debe mostrar directamente los valores snake_case del
 * backend. Los mapas cerrados obligan a decidir título, explicación y tono
 * para cada estado antes de poder incorporarlo a la interfaz.
 */
export const businessVerificationPresentation = {
  unverified: {
    titleKey: "business.statuses.unverified.title",
    descriptionKey: "business.statuses.unverified.description",
    tone: "neutral",
  },
  pending_remote_check: {
    titleKey: "business.statuses.pendingRemoteCheck.title",
    descriptionKey: "business.statuses.pendingRemoteCheck.description",
    tone: "info",
  },
  verified: {
    titleKey: "business.statuses.verified.title",
    descriptionKey: "business.statuses.verified.description",
    tone: "success",
  },
  pending_review: {
    titleKey: "business.statuses.pendingReview.title",
    descriptionKey: "business.statuses.pendingReview.description",
    tone: "warning",
  },
  rejected: {
    titleKey: "business.statuses.rejected.title",
    descriptionKey: "business.statuses.rejected.description",
    tone: "danger",
  },
  expired: {
    titleKey: "business.statuses.expired.title",
    descriptionKey: "business.statuses.expired.description",
    tone: "warning",
  },
} as const satisfies Record<BusinessVerificationStatus, VerificationStatusPresentation>;

export const emailVerificationPresentation = {
  pending: {
    titleKey: "email.statuses.pending.title",
    descriptionKey: "email.statuses.pending.description",
    tone: "warning",
  },
  verified: {
    titleKey: "email.statuses.verified.title",
    descriptionKey: "email.statuses.verified.description",
    tone: "success",
  },
} as const satisfies Record<EmailVerificationStatus, VerificationStatusPresentation>;

export const manualReviewPresentation = {
  pending_review: {
    titleKey: "manualReview.statuses.pendingReview.title",
    descriptionKey: "manualReview.statuses.pendingReview.description",
    tone: "warning",
  },
  approved: {
    titleKey: "manualReview.statuses.approved.title",
    descriptionKey: "manualReview.statuses.approved.description",
    tone: "success",
  },
  rejected: {
    titleKey: "manualReview.statuses.rejected.title",
    descriptionKey: "manualReview.statuses.rejected.description",
    tone: "danger",
  },
  needs_correction: {
    titleKey: "manualReview.statuses.needsCorrection.title",
    descriptionKey: "manualReview.statuses.needsCorrection.description",
    tone: "info",
  },
} as const satisfies Record<ManualReviewStatus, VerificationStatusPresentation>;

export const documentReviewPresentation = {
  pending_review: {
    titleKey: "documents.statuses.pendingReview.title",
    descriptionKey: "documents.statuses.pendingReview.description",
    tone: "warning",
  },
  accepted: {
    titleKey: "documents.statuses.accepted.title",
    descriptionKey: "documents.statuses.accepted.description",
    tone: "success",
  },
  rejected: {
    titleKey: "documents.statuses.rejected.title",
    descriptionKey: "documents.statuses.rejected.description",
    tone: "danger",
  },
  needs_correction: {
    titleKey: "documents.statuses.needsCorrection.title",
    descriptionKey: "documents.statuses.needsCorrection.description",
    tone: "info",
  },
} as const satisfies Record<DocumentReviewStatus, VerificationStatusPresentation>;
