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
  verificationStatus: z.literal("pending_review"),
  verificationProvider: z.string().nullable(),
  verificationReference: z.string().nullable(),
  manualReviewStatus: z.literal("pending_review"),
  updatedAt: z.iso.datetime({ offset: true }),
});
const businessAccountsSchema = z.object({
  accounts: z.array(businessAccountSchema).max(100),
});
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
