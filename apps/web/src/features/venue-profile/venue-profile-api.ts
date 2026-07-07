import { z } from "zod";

import { supportedLocales } from "@/i18n/config";

import { localizedTextSchema, type VenueProfilePayload } from "./venue-profile-schema";

const venueCategorySchema = z.object({
  id: z.uuid(),
  slug: z.string().min(1),
  name: z.string().min(1),
});

const venueProfileSchema = z.object({
  id: z.uuid(),
  categoryId: z.uuid(),
  categorySlug: z.string().min(1),
  categoryName: z.string().min(1),
  name: z.string().min(1),
  slug: z.string().min(1),
  description: z.string().nullable(),
  descriptionI18n: localizedTextSchema,
  servicesI18n: localizedTextSchema,
  rulesI18n: localizedTextSchema,
  publicTextI18n: localizedTextSchema,
  defaultLocale: z.enum(supportedLocales),
  contactEmail: z.string().nullable(),
  phone: z.string().nullable(),
  address: z.string().nullable(),
  city: z.string().nullable(),
  province: z.string().nullable(),
  country: z.string().nullable(),
  postalCode: z.string().nullable(),
  latitude: z.number().nullable(),
  longitude: z.number().nullable(),
  mainImageUrl: z.string().nullable(),
  status: z.string(),
  showPhone: z.boolean(),
  showEmail: z.boolean(),
  createdAt: z.iso.datetime(),
  updatedAt: z.iso.datetime(),
});

const galleryImageSchema = z.object({
  id: z.uuid(),
  url: z.string().min(1),
  altText: z.string().min(1),
  position: z.number().int().nonnegative(),
  mediaType: z.string().min(1),
  sizeBytes: z.number().int().nonnegative(),
  width: z.number().int().positive(),
  height: z.number().int().positive(),
});

const publicationErrorSchema = z.object({
  error: z.literal("VENUE_PUBLICATION_REJECTED"),
  requirements: z.array(z.string()),
});

export type VenueCategory = z.infer<typeof venueCategorySchema>;
export type VenueProfile = z.infer<typeof venueProfileSchema>;
export type VenueGalleryImage = z.infer<typeof galleryImageSchema>;
export type VenueProfileApiErrorKind =
  | "unauthenticated"
  | "forbidden"
  | "notFound"
  | "conflict"
  | "invalid"
  | "descriptionTooLong"
  | "publicationRejected"
  | "imageInvalid"
  | "galleryLimit"
  | "rateLimited"
  | "unavailable";

export class VenueProfileApiError extends Error {
  constructor(
    public readonly kind: VenueProfileApiErrorKind,
    public readonly requirements: string[] = [],
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueProfileApiError";
  }
}

/** Cliente del panel privado de perfil; siempre usa cookies HttpOnly del navegador. */
export async function fetchVenueProfile(signal?: AbortSignal): Promise<VenueProfile | null> {
  const response = await request("/api/venue/me", { method: "GET", signal });
  if (response.status === 404) {
    return null;
  }
  await throwForStatus(response);
  return parseJson(response, venueProfileSchema);
}

export async function fetchVenueCategories(
  locale: string,
  signal?: AbortSignal,
): Promise<VenueCategory[]> {
  const response = await request(`/api/public/categories?locale=${encodeURIComponent(locale)}`, {
    method: "GET",
    signal,
  });
  await throwForStatus(response);
  return z.array(venueCategorySchema).parse(await response.json());
}

export async function saveVenueProfile(
  payload: VenueProfilePayload,
  exists: boolean,
  signal?: AbortSignal,
): Promise<VenueProfile> {
  const response = await request("/api/venue/me/profile", {
    method: exists ? "PATCH" : "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
    signal,
  });
  await throwForStatus(response);
  return parseJson(response, venueProfileSchema);
}

export async function publishVenueProfile(signal?: AbortSignal): Promise<VenueProfile> {
  const response = await request("/api/venue/me/publish", { method: "POST", signal });
  await throwForStatus(response);
  return parseJson(response, venueProfileSchema);
}

export async function uploadMainImage(file: File, signal?: AbortSignal): Promise<VenueProfile> {
  const body = new FormData();
  body.set("file", file);
  const response = await request("/api/venue/me/main-image", { method: "POST", body, signal });
  await throwForStatus(response);
  return parseJson(response, venueProfileSchema);
}

export async function fetchVenueGallery(signal?: AbortSignal): Promise<VenueGalleryImage[]> {
  const response = await request("/api/venue/me/gallery", { method: "GET", signal });
  if (response.status === 404) {
    return [];
  }
  await throwForStatus(response);
  return z.array(galleryImageSchema).parse(await response.json());
}

export async function uploadGalleryImage(
  file: File,
  altText: string,
  signal?: AbortSignal,
): Promise<VenueGalleryImage> {
  const body = new FormData();
  body.set("altText", altText);
  body.set("file", file);
  const response = await request("/api/venue/me/gallery", { method: "POST", body, signal });
  await throwForStatus(response);
  return parseJson(response, galleryImageSchema);
}

export async function deleteGalleryImage(imageId: string, signal?: AbortSignal): Promise<void> {
  const response = await request(`/api/venue/me/gallery/${imageId}`, { method: "DELETE", signal });
  await throwForStatus(response);
}

export function resolveVenueAssetUrl(path: string | null): string | null {
  if (!path) {
    return null;
  }
  if (/^https?:\/\//i.test(path)) {
    return path;
  }
  return `${apiBaseUrl().replace(/\/$/, "")}${path.startsWith("/") ? path : `/${path}`}`;
}

async function request(path: string, init: RequestInit) {
  try {
    return await fetch(`${apiBaseUrl().replace(/\/$/, "")}${path}`, {
      credentials: "include",
      headers: { Accept: "application/json", ...init.headers },
      ...init,
    });
  } catch (error) {
    throw new VenueProfileApiError("unavailable", [], { cause: error });
  }
}

async function throwForStatus(response: Response) {
  if (response.ok) {
    return;
  }
  if (response.status === 422) {
    const body = await safeJson(response);
    const publication = publicationErrorSchema.safeParse(body);
    if (publication.success) {
      throw new VenueProfileApiError("publicationRejected", publication.data.requirements);
    }
    if (body?.error === "VENUE_DESCRIPTION_TOO_LONG") {
      throw new VenueProfileApiError("descriptionTooLong");
    }
  }
  const byStatus: Partial<Record<number, VenueProfileApiErrorKind>> = {
    400: "invalid",
    401: "unauthenticated",
    403: "forbidden",
    404: "notFound",
    409: "conflict",
    429: "rateLimited",
  };
  const byError: Record<string, VenueProfileApiErrorKind> = {
    VENUE_IMAGE_INVALID: "imageInvalid",
    VENUE_GALLERY_LIMIT_REACHED: "galleryLimit",
  };
  const body = await safeJson(response);
  const kind: VenueProfileApiErrorKind | undefined =
    (body?.error ? byError[String(body.error)] : undefined) ?? byStatus[response.status];
  throw new VenueProfileApiError(kind ?? "unavailable");
}

async function parseJson<T>(response: Response, schema: z.ZodType<T>): Promise<T> {
  try {
    return schema.parse(await response.json());
  } catch (error) {
    throw new VenueProfileApiError("unavailable", [], { cause: error });
  }
}

async function safeJson(response: Response): Promise<Record<string, unknown> | null> {
  try {
    const body = await response.clone().json();
    return typeof body === "object" && body !== null ? (body as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!value) {
    throw new VenueProfileApiError("unavailable");
  }
  return value;
}
