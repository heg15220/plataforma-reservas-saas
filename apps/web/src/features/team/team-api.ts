import { z } from "zod";

const nullableText = z.string().nullable();
const localizedTextSchema = z
  .object({ sourceLocale: z.string(), values: z.record(z.string(), z.string()) })
  .nullable();

const resourceSchema = z.object({
  id: z.uuid(),
  type: z.enum(["employee", "professional", "room", "court", "table", "equipment", "other"]),
  firstName: nullableText,
  lastName: nullableText,
  publicAlias: nullableText,
  photoUrl: nullableText,
  specialty: nullableText,
  description: nullableText,
  status: z.enum(["active", "inactive", "internal_only", "archived"]),
  publicVisibility: z.boolean(),
  internalNotes: nullableText,
  createdAt: z.string(),
  updatedAt: z.string(),
});

const weeklyHourSchema = z.object({
  id: z.uuid(),
  weekday: z.number().int().min(1).max(7),
  available: z.boolean(),
  startsAt: nullableText,
  endsAt: nullableText,
  createdAt: z.string(),
  updatedAt: z.string(),
});

const serviceSchema = z.object({
  id: z.uuid(),
  name: z.string().min(1),
  nameI18n: localizedTextSchema,
  description: nullableText,
  descriptionI18n: localizedTextSchema,
  durationMinutes: z.number().int().positive(),
  capacityRequired: z.number().int().positive(),
  active: z.boolean(),
  allowsAnyAvailableResource: z.boolean(),
  employeeResourceIds: z.array(z.uuid()),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export type EmployeeResource = z.infer<typeof resourceSchema>;
export type EmployeeResourceType = EmployeeResource["type"];
export type EmployeeResourceStatus = EmployeeResource["status"];
export type WeeklyHour = z.infer<typeof weeklyHourSchema>;
export type VenueService = z.infer<typeof serviceSchema>;

export interface EmployeeResourceInput {
  type: EmployeeResourceType;
  firstName: string | null;
  lastName: string | null;
  publicAlias: string | null;
  photoUrl: string | null;
  specialty: string | null;
  description: string | null;
  status: EmployeeResourceStatus;
  publicVisibility: boolean;
  internalNotes: string | null;
}

export interface WeeklyHourInput {
  weekday: number;
  available: boolean;
  startsAt: string | null;
  endsAt: string | null;
}

export interface VenueServiceInput {
  name: string;
  nameI18n: null;
  description: string | null;
  descriptionI18n: null;
  durationMinutes: number;
  capacityRequired: number;
  active: boolean;
  allowsAnyAvailableResource: boolean;
}

export type TeamApiErrorKind =
  | "unauthenticated"
  | "forbidden"
  | "notFound"
  | "conflict"
  | "invalid"
  | "unavailable";

export class TeamApiError extends Error {
  constructor(
    public readonly kind: TeamApiErrorKind,
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "TeamApiError";
  }
}

/** Lista recursos no archivados del local autenticado. */
export async function fetchEmployeeResources(signal?: AbortSignal) {
  return parse(
    await privateRequest("/api/venue/me/team", { method: "GET", signal }),
    z.array(resourceSchema),
  );
}

/** Crea un recurso y devuelve la representacion reconciliada por backend. */
export async function createEmployeeResource(input: EmployeeResourceInput) {
  return parse(
    await privateRequest("/api/venue/me/team", jsonRequest("POST", input)),
    resourceSchema,
  );
}

/** Actualiza todos los campos editables de un recurso propio. */
export async function updateEmployeeResource(id: string, input: EmployeeResourceInput) {
  return parse(
    await privateRequest(`/api/venue/me/team/${encodeURIComponent(id)}`, jsonRequest("PATCH", input)),
    resourceSchema,
  );
}

export async function fetchWeeklyHours(id: string, signal?: AbortSignal) {
  return parse(
    await privateRequest(`/api/venue/me/team/${encodeURIComponent(id)}/weekly-hours`, {
      method: "GET",
      signal,
    }),
    z.array(weeklyHourSchema),
  );
}

/** Reemplaza la configuracion semanal completa del recurso. */
export async function saveWeeklyHours(id: string, hours: WeeklyHourInput[]) {
  return parse(
    await privateRequest(
      `/api/venue/me/team/${encodeURIComponent(id)}/weekly-hours`,
      jsonRequest("PUT", { hours }),
    ),
    z.array(weeklyHourSchema),
  );
}

export async function fetchVenueServices(signal?: AbortSignal) {
  return parse(
    await privateRequest("/api/venue/me/services", { method: "GET", signal }),
    z.array(serviceSchema),
  );
}

export async function createVenueService(input: VenueServiceInput) {
  return parse(
    await privateRequest("/api/venue/me/services", jsonRequest("POST", input)),
    serviceSchema,
  );
}

export async function updateVenueService(id: string, input: VenueServiceInput) {
  return parse(
    await privateRequest(
      `/api/venue/me/services/${encodeURIComponent(id)}`,
      jsonRequest("PATCH", input),
    ),
    serviceSchema,
  );
}

/** Reemplaza de forma idempotente las compatibilidades servicio-recurso. */
export async function saveServiceResources(id: string, resourceIds: string[]) {
  return parse(
    await privateRequest(
      `/api/venue/me/services/${encodeURIComponent(id)}/resources`,
      jsonRequest("PUT", { resourceIds }),
    ),
    serviceSchema,
  );
}

function jsonRequest(method: string, body: unknown): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  };
}

async function privateRequest(path: string, init: RequestInit) {
  let response: Response;
  try {
    response = await fetch(new URL(path, apiBaseUrl()), {
      credentials: "include",
      headers: { Accept: "application/json", ...init.headers },
      ...init,
    });
  } catch (error) {
    throw new TeamApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus: Partial<Record<number, TeamApiErrorKind>> = {
      400: "invalid",
      401: "unauthenticated",
      403: "forbidden",
      404: "notFound",
      409: "conflict",
      422: "invalid",
    };
    throw new TeamApiError(byStatus[response.status] ?? "unavailable");
  }
  return response;
}

async function parse<T>(response: Response, schema: z.ZodType<T>): Promise<T> {
  try {
    return schema.parse(await response.json());
  } catch (error) {
    throw new TeamApiError("unavailable", { cause: error });
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!value) throw new TeamApiError("unavailable");
  return value.endsWith("/") ? value : `${value}/`;
}