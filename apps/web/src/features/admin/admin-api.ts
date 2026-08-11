import { z } from "zod";

const categorySchema = z.object({
  id: z.uuid(),
  slug: z.string(),
  nameEs: z.string(),
  nameEn: z.string(),
  active: z.boolean(),
  updatedAt: z.iso.datetime({ offset: true }),
});
const venueSchema = z.object({
  id: z.uuid(),
  name: z.string(),
  slug: z.string(),
  categoryId: z.uuid(),
  categoryName: z.string(),
  status: z.string(),
  contactEmail: z.string().nullable(),
  phone: z.string().nullable(),
  address: z.string().nullable(),
  city: z.string().nullable(),
  province: z.string().nullable(),
  country: z.string().nullable(),
  postalCode: z.string().nullable(),
  updatedAt: z.iso.datetime({ offset: true }),
});
const categoriesSchema = z.object({ categories: z.array(categorySchema).max(500) });
const venuesSchema = z.object({ venues: z.array(venueSchema).max(100) });
const incidentSchema = z.object({
  id: z.uuid(),
  reservationId: z.uuid(),
  venueId: z.uuid(),
  venueName: z.string().nullable(),
  customerEmailNormalized: z.email(),
  incidentType: z.string(),
  reportedByUserId: z.uuid(),
  reportedAt: z.iso.datetime({ offset: true }),
  notes: z.string().nullable(),
  status: z.enum(["reported", "confirmed", "dismissed"]),
});
const incidentsSchema = z.object({ incidents: z.array(incidentSchema).max(100) });
const businessAccountSchema = z.object({
  id: z.uuid(),
  ownerUserId: z.uuid(),
  ownerEmail: z.email(),
  taxCountry: z.string().length(2),
  businessLegalName: z.string(),
  businessTaxIdentifier: z.string(),
  businessAddress: z.string().nullable(),
  verificationStatus: z.enum([
    "unverified",
    "pending_remote_check",
    "verified",
    "pending_review",
    "rejected",
    "expired",
  ]),
  verificationProvider: z.string().nullable(),
  manualReviewStatus: z
    .enum(["pending_review", "approved", "rejected", "needs_correction"])
    .nullable(),
  updatedAt: z.iso.datetime({ offset: true }),
});
const businessAccountsSchema = z.object({
  accounts: z.array(businessAccountSchema).max(100),
});
const documentSchema = z.object({
  id: z.uuid(),
  businessAccountId: z.uuid(),
  documentRequestId: z.uuid().nullable(),
  documentType: z.string(),
  mediaType: z.string().nullable(),
  fileSizeBytes: z.number().int().nonnegative().nullable(),
  malwareScanStatus: z.string().nullable(),
  status: z.enum(["pending_review", "accepted", "rejected", "needs_correction"]),
  createdAt: z.iso.datetime({ offset: true }),
  reviewedAt: z.iso.datetime({ offset: true }).nullable(),
  reviewNotes: z.string().nullable(),
});
const documentsSchema = z.object({ documents: z.array(documentSchema).max(100) });
const penaltySchema = z.object({
  id: z.uuid(),
  customerEmailNormalized: z.email(),
  scope: z.enum(["global", "venue"]),
  venueId: z.uuid().nullable(),
  incidentCountOperational: z.number().int().positive(),
  startsAt: z.iso.datetime({ offset: true }),
  endsAt: z.iso.datetime({ offset: true }),
  status: z.enum(["active", "expired", "revoked"]),
  reason: z.string(),
  createdFromIncidentId: z.uuid(),
  updatedAt: z.iso.datetime({ offset: true }),
});
const penaltiesSchema = z.object({ penalties: z.array(penaltySchema).max(100) });
const planFeatureSchema = z.object({
  code: z.string(),
  labelEs: z.string(),
  labelEn: z.string(),
});
const planLimitsSchema = z.object({
  monthlyReservations: z.number().int().nonnegative().nullable(),
  teamResources: z.number().int().nonnegative().nullable(),
  customFormFields: z.number().int().nonnegative().nullable(),
  galleryImages: z.number().int().nonnegative().nullable(),
});
const planSchema = z.object({
  id: z.uuid(),
  slug: z.string(),
  nameEs: z.string(),
  nameEn: z.string(),
  priceMonthly: z.number().nonnegative(),
  priceYearly: z.number().nonnegative(),
  limits: planLimitsSchema,
  features: z.array(planFeatureSchema).max(50),
  active: z.boolean(),
  updatedAt: z.iso.datetime({ offset: true }),
});
const plansSchema = z.object({ plans: z.array(planSchema).max(100) });
const metricsSchema = z.object({
  totalVenues: z.number().int().nonnegative(),
  publishedVenues: z.number().int().nonnegative(),
  suspendedVenues: z.number().int().nonnegative(),
  totalReservations: z.number().int().nonnegative(),
  confirmedReservations: z.number().int().nonnegative(),
  totalBusinessAccounts: z.number().int().nonnegative(),
  pendingBusinessReviews: z.number().int().nonnegative(),
  activeSubscriptions: z.number().int().nonnegative(),
  activePenalties: z.number().int().nonnegative(),
  generatedAt: z.iso.datetime({ offset: true }),
});
const auditLogSchema = z.object({
  id: z.uuid(),
  actorUserId: z.uuid().nullable(),
  actorRole: z.string(),
  entityType: z.string(),
  entityId: z.uuid().nullable(),
  action: z.string(),
  before: z.record(z.string(), z.unknown()).nullable(),
  after: z.record(z.string(), z.unknown()).nullable(),
  createdAt: z.iso.datetime({ offset: true }),
});
const auditLogsSchema = z.object({ logs: z.array(auditLogSchema).max(100) });
const loginSchema = z.object({
  userId: z.uuid(),
  accountType: z.literal("admin"),
  preferredLocale: z.enum(["es", "en"]),
  emailVerified: z.boolean(),
  sessionExpiresAt: z.iso.datetime({ offset: true }),
});

export type AdminCategory = z.infer<typeof categorySchema>;
export type AdminVenue = z.infer<typeof venueSchema>;
export type AdminIncident = z.infer<typeof incidentSchema>;
export type AdminBusinessAccount = z.infer<typeof businessAccountSchema>;
export type AdminDocument = z.infer<typeof documentSchema>;
export type AdminPenalty = z.infer<typeof penaltySchema>;
export type AdminPlan = z.infer<typeof planSchema>;
export type AdminPlanInput = Omit<AdminPlan, "id" | "updatedAt">;
export type AdminMetrics = z.infer<typeof metricsSchema>;
export type AdminAuditLog = z.infer<typeof auditLogSchema>;
export type AdminCategoryInput = Pick<AdminCategory, "active" | "nameEn" | "nameEs" | "slug">;
export type AdminVenueInput = Pick<
  AdminVenue,
  | "address"
  | "categoryId"
  | "city"
  | "contactEmail"
  | "country"
  | "name"
  | "phone"
  | "postalCode"
  | "province"
>;

export class AdminApiError extends Error {
  constructor(public readonly kind: "forbidden" | "invalid" | "unavailable") {
    super(kind);
  }
}

/** Autentica exclusivamente cuentas admin y recibe la cookie HttpOnly. */
export async function loginAdmin(email: string, password: string) {
  return request(
    "/api/auth/admin/login",
    loginSchema,
    { method: "POST", body: JSON.stringify({ email, password }) },
    true,
  );
}

export async function fetchAdminCategories(signal?: AbortSignal) {
  return request("/api/admin/categories", categoriesSchema, { signal });
}

export async function saveAdminCategory(input: AdminCategoryInput, categoryId?: string) {
  return request(
    categoryId ? `/api/admin/categories/${categoryId}` : "/api/admin/categories",
    categorySchema,
    { method: categoryId ? "PATCH" : "POST", body: JSON.stringify(input) },
  );
}

export async function fetchAdminVenues(signal?: AbortSignal) {
  return request("/api/admin/venues", venuesSchema, { signal });
}

export async function saveAdminVenue(venueId: string, input: AdminVenueInput) {
  return request(`/api/admin/venues/${venueId}`, venueSchema, {
    method: "PATCH",
    body: JSON.stringify(input),
  });
}

/** Suspende un local mediante una acción separada de la edición básica. */
export async function suspendAdminVenue(venueId: string, reason: string) {
  return request(`/api/admin/venues/${venueId}/suspension`, venueSchema, {
    method: "PATCH",
    body: JSON.stringify({ reason }),
  });
}

export async function fetchAdminIncidents(signal?: AbortSignal) {
  return request("/api/admin/incidents", incidentsSchema, { signal });
}

/** Confirma o desestima una incidencia; la API exige siempre un motivo auditable. */
export async function reviewAdminIncident(
  incidentId: string,
  status: "confirmed" | "dismissed",
  reason: string,
) {
  return request(`/api/admin/incidents/${incidentId}`, incidentSchema, {
    method: "PATCH",
    body: JSON.stringify({ status, reason }),
  });
}

export async function fetchPendingBusinessAccounts(signal?: AbortSignal) {
  return request("/api/admin/business-accounts", businessAccountsSchema, { signal });
}

export async function decideBusinessAccount(
  accountId: string,
  decision: "approved" | "rejected",
  reason: string,
) {
  return request(
    `/api/admin/business-accounts/${accountId}/${decision === "approved" ? "approve" : "reject"}`,
    businessAccountSchema,
    {
      method: "POST",
      body: JSON.stringify({ reason }),
    },
  );
}

export async function recheckBusinessAccount(accountId: string, reason: string) {
  return request(`/api/admin/business-accounts/${accountId}/recheck`, businessAccountSchema, {
    method: "POST",
    body: JSON.stringify({ requestId: crypto.randomUUID(), reason }),
  });
}

export async function fetchPendingDocuments(signal?: AbortSignal) {
  return request("/api/admin/business-documents", documentsSchema, { signal });
}

/** Recupera contenido privado con credenciales; el llamador controla su URL efímera en memoria. */
export async function fetchAdminDocumentContent(documentId: string) {
  let response: Response;
  try {
    response = await fetch(
      new URL(`/api/admin/business-documents/${documentId}/content`, apiBase()),
      { credentials: "include", headers: { Accept: "application/octet-stream" } },
    );
  } catch {
    throw new AdminApiError("unavailable");
  }
  if (response.status === 401 || response.status === 403) throw new AdminApiError("forbidden");
  if (!response.ok) throw new AdminApiError("unavailable");
  return response.blob();
}

export async function reviewAdminDocument(
  documentId: string,
  decision: "accepted" | "rejected" | "needs_correction",
  reason: string,
) {
  return request(`/api/admin/business-documents/${documentId}`, documentSchema, {
    method: "PATCH",
    body: JSON.stringify({ decision, reason }),
  });
}

export async function fetchAdminPenalties(signal?: AbortSignal) {
  return request("/api/admin/penalties", penaltiesSchema, { signal });
}

export async function updateAdminPenalty(
  penaltyId: string,
  status: "active" | "revoked",
  endsAt: string | null,
  reason: string,
) {
  return request(`/api/admin/penalties/${penaltyId}`, penaltySchema, {
    method: "PATCH",
    body: JSON.stringify({ status, endsAt, reason }),
  });
}

export async function fetchAdminPlans(signal?: AbortSignal) {
  return request("/api/admin/plans", plansSchema, { signal });
}

/** Conserva el mismo contrato bilingüe tanto al crear como al editar planes. */
export async function saveAdminPlan(input: AdminPlanInput, planId?: string) {
  return request(planId ? `/api/admin/plans/${planId}` : "/api/admin/plans", planSchema, {
    method: planId ? "PATCH" : "POST",
    body: JSON.stringify(input),
  });
}

export async function fetchAdminMetrics(signal?: AbortSignal) {
  return request("/api/admin/metrics", metricsSchema, { signal });
}

export async function fetchAdminAuditLogs(signal?: AbortSignal) {
  return request("/api/admin/audit-logs", auditLogsSchema, { signal });
}

async function request<T>(
  path: string,
  schema: z.ZodType<T>,
  init: RequestInit = {},
  login = false,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(new URL(path, apiBase()), {
      ...init,
      credentials: "include",
      headers: { Accept: "application/json", "Content-Type": "application/json", ...init.headers },
    });
  } catch {
    throw new AdminApiError("unavailable");
  }
  if (response.status === 401 || response.status === 403 || (login && response.status === 400)) {
    throw new AdminApiError(login ? "invalid" : "forbidden");
  }
  if (!response.ok) throw new AdminApiError("unavailable");
  try {
    return schema.parse(await response.json());
  } catch {
    throw new AdminApiError("unavailable");
  }
}

function apiBase() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!value) throw new AdminApiError("unavailable");
  return value.endsWith("/") ? value : `${value}/`;
}
