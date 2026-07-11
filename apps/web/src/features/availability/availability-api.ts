import { z } from "zod";

const isoDateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/);
const localTimeSchema = z.string().regex(/^\d{2}:\d{2}(?::\d{2})?$/);

const publicSlotSchema = z.object({
  slotId: z.uuid(),
  startsAt: localTimeSchema,
  endsAt: localTimeSchema,
  capacity: z.number().int().positive(),
  availableCapacity: z.number().int().nonnegative(),
  status: z.string().min(1),
  bookingAvailable: z.boolean(),
});

const publicAvailabilitySchema = z.object({
  venueSlug: z.string().min(1),
  date: isoDateSchema,
  weekday: z.number().int().min(1).max(7),
  statusCode: z.enum(["open", "closed", "unavailable", "full", "upcoming_available"]),
  statusLabel: z.string().min(1),
  bookingAvailable: z.boolean(),
  closed: z.boolean(),
  reservationsEnabled: z.boolean(),
  source: z.string().min(1),
  availableSlotCount: z.number().int().nonnegative(),
  slots: z.array(publicSlotSchema),
});

const openingHourSchema = z.object({
  id: z.uuid().nullable(),
  weekday: z.number().int().min(1).max(7),
  closed: z.boolean(),
  reservationsEnabled: z.boolean(),
  opensAt: localTimeSchema.nullable(),
  closesAt: localTimeSchema.nullable(),
});

const openingHoursSchema = z.object({ days: z.array(openingHourSchema).length(7) });

const availabilityDaySchema = z.object({
  date: isoDateSchema,
  closed: z.boolean(),
  reservationsEnabled: z.boolean(),
  source: z.string().min(1),
  blockId: z.uuid().nullable(),
  reason: z.string().nullable(),
});

const timeSlotSchema = z.object({
  id: z.uuid(),
  date: isoDateSchema,
  weekday: z.number().int().min(1).max(7),
  startsAt: localTimeSchema,
  endsAt: localTimeSchema,
  capacity: z.number().int().positive(),
  status: z.string().min(1),
  createdByRule: z.boolean(),
  version: z.number().int().nonnegative(),
});

export type PublicAvailability = z.infer<typeof publicAvailabilitySchema>;
export type OpeningHour = z.infer<typeof openingHourSchema>;
export type AvailabilityDay = z.infer<typeof availabilityDaySchema>;
export type TimeSlot = z.infer<typeof timeSlotSchema>;

export interface OpeningHourInput {
  weekday: number;
  closed: boolean;
  reservationsEnabled: boolean;
  opensAt: string | null;
  closesAt: string | null;
}

export interface AvailabilityDayInput {
  date: string;
  closed: boolean;
  reservationsEnabled: boolean;
  reason: string | null;
}

export type AvailabilityApiErrorKind =
  | "unauthenticated"
  | "forbidden"
  | "notFound"
  | "conflict"
  | "invalid"
  | "unavailable";

export class AvailabilityApiError extends Error {
  constructor(
    public readonly kind: AvailabilityApiErrorKind,
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "AvailabilityApiError";
  }
}

/** Consulta anónima de una fecha; no reenvía credenciales ni estado privado. */
export async function fetchPublicAvailability(
  slug: string,
  date: string,
  locale: string,
  signal?: AbortSignal,
): Promise<PublicAvailability> {
  const path = `/api/public/venues/${encodeURIComponent(slug)}/availability`;
  const url = new URL(path, apiBaseUrl());
  url.searchParams.set("date", date);
  url.searchParams.set("locale", locale);
  return parse(
    await request(url.toString(), { method: "GET", signal }, false),
    publicAvailabilitySchema,
  );
}

/** Lee el horario semanal del local autenticado mediante su cookie HttpOnly. */
export async function fetchOpeningHours(signal?: AbortSignal): Promise<OpeningHour[]> {
  const response = await privateRequest("/api/venue/me/opening-hours", { method: "GET", signal });
  return parse(response, openingHoursSchema).then((value) => value.days);
}

/** Reemplaza atómicamente los siete días del horario semanal. */
export async function saveOpeningHours(days: OpeningHourInput[]): Promise<OpeningHour[]> {
  const response = await privateRequest(
    "/api/venue/me/opening-hours",
    jsonRequest("PUT", { days }),
  );
  return parse(response, openingHoursSchema).then((value) => value.days);
}

/** Consulta la excepción efectiva de una fecha del local autenticado. */
export async function fetchAvailabilityDay(date: string, signal?: AbortSignal) {
  const url = new URL("/api/venue/me/availability-days", apiBaseUrl());
  url.searchParams.set("date", date);
  return parse(
    await request(url.toString(), { method: "GET", signal }, true),
    availabilityDaySchema,
  );
}

/** Guarda o elimina implícitamente la excepción de una fecha según el payload. */
export async function saveAvailabilityDay(input: AvailabilityDayInput) {
  return parse(
    await privateRequest("/api/venue/me/availability-days", jsonRequest("PUT", input)),
    availabilityDaySchema,
  );
}

/** Lista las franjas privadas de una fecha sin exponer IDs de otros locales. */
export async function fetchTimeSlots(date: string, signal?: AbortSignal) {
  const url = new URL("/api/venue/me/time-slots", apiBaseUrl());
  url.searchParams.set("date", date);
  return parse(
    await request(url.toString(), { method: "GET", signal }, true),
    z.array(timeSlotSchema),
  );
}

export async function createTimeSlot(input: {
  date: string;
  startsAt: string;
  endsAt: string;
  capacity: number;
}) {
  return parse(
    await privateRequest("/api/venue/me/time-slots", jsonRequest("POST", input)),
    timeSlotSchema,
  );
}

export async function generateTimeSlots(input: {
  date: string;
  durationMinutes: number;
  capacity: number;
}) {
  return parse(
    await privateRequest("/api/venue/me/time-slots/generate", jsonRequest("POST", input)),
    z.array(timeSlotSchema),
  );
}

export async function updateTimeSlotCapacity(slotId: string, capacity: number) {
  return parse(
    await privateRequest(
      `/api/venue/me/time-slots/${encodeURIComponent(slotId)}/capacity`,
      jsonRequest("PATCH", { capacity }),
    ),
    timeSlotSchema,
  );
}

export async function setTimeSlotBlocked(slotId: string, blocked: boolean) {
  const action = blocked ? "block" : "reopen";
  return parse(
    await privateRequest(`/api/venue/me/time-slots/${encodeURIComponent(slotId)}/${action}`, {
      method: "PATCH",
    }),
    timeSlotSchema,
  );
}

function jsonRequest(method: string, body: unknown): RequestInit {
  return {
    method,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  };
}

function privateRequest(path: string, init: RequestInit) {
  return request(new URL(path, apiBaseUrl()).toString(), init, true);
}

async function request(url: string, init: RequestInit, authenticated: boolean) {
  let response: Response;
  try {
    response = await fetch(url, {
      credentials: authenticated ? "include" : "omit",
      headers: { Accept: "application/json", ...init.headers },
      ...init,
    });
  } catch (error) {
    throw new AvailabilityApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus: Partial<Record<number, AvailabilityApiErrorKind>> = {
      400: "invalid",
      401: "unauthenticated",
      403: "forbidden",
      404: "notFound",
      409: "conflict",
      422: "invalid",
    };
    throw new AvailabilityApiError(byStatus[response.status] ?? "unavailable");
  }
  return response;
}

async function parse<T>(response: Response, schema: z.ZodType<T>): Promise<T> {
  try {
    return schema.parse(await response.json());
  } catch (error) {
    throw new AvailabilityApiError("unavailable", { cause: error });
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (!value) {
    throw new AvailabilityApiError("unavailable");
  }
  return value.endsWith("/") ? value : `${value}/`;
}
