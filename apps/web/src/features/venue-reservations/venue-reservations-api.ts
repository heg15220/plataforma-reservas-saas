import { z } from "zod";

const isoDateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/);
const localTimeSchema = z.string().regex(/^\d{2}:\d{2}(?::\d{2})?$/);
const instantSchema = z.string().datetime();

const reservationSummarySchema = z.object({
  id: z.uuid(),
  timeSlotId: z.uuid(),
  customerName: z.string().min(1),
  customerEmail: z.email(),
  partySize: z.number().int().positive(),
  date: isoDateSchema,
  startsAt: localTimeSchema,
  endsAt: localTimeSchema,
  status: z.string().min(1),
  createdAt: instantSchema,
});

const reservationListSchema = z.object({
  items: z.array(reservationSummarySchema),
  page: z.number().int().nonnegative(),
  size: z.number().int().positive(),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
});

const formAnswerSchema = z.object({
  fieldKey: z.string().min(1),
  fieldLabel: z.string().min(1),
  value: z.unknown(),
  createdAt: instantSchema,
});

const assignedResourceSchema = z.object({
  id: z.uuid(),
  type: z.string().min(1),
  firstName: z.string().nullable(),
  lastName: z.string().nullable(),
  publicAlias: z.string().nullable(),
  specialty: z.string().nullable(),
  status: z.string().min(1),
});

const incidentSchema = z.object({
  incidentType: z.string().min(1),
  reportedAt: instantSchema,
  status: z.string().min(1),
});

const incidentHistorySchema = z.object({
  totalElements: z.number().int().nonnegative(),
  truncated: z.boolean(),
  items: z.array(incidentSchema).max(50),
});

const reservationDetailSchema = reservationSummarySchema.omit({ createdAt: true }).extend({
  serviceId: z.uuid().nullable(),
  cancelledAt: instantSchema.nullable(),
  cancelledBy: z.string().nullable(),
  cancellationReason: z.string().nullable(),
  createdAt: instantSchema,
  updatedAt: instantSchema,
  formAnswers: z.array(formAnswerSchema),
  assignedResource: assignedResourceSchema.nullable(),
  incidentHistory: incidentHistorySchema,
});

const cancellationSchema = z.object({
  reservationId: z.uuid(),
  status: z.literal("cancelled_by_venue"),
  cancelledAt: instantSchema,
});

export type VenueReservationSummary = z.infer<typeof reservationSummarySchema>;
export type VenueReservationList = z.infer<typeof reservationListSchema>;
export type VenueReservationDetail = z.infer<typeof reservationDetailSchema>;
export type VenueReservationCancellation = z.infer<typeof cancellationSchema>;

export type VenueReservationsApiErrorKind =
  | "unauthenticated"
  | "forbidden"
  | "notFound"
  | "invalid"
  | "unavailable";

/** Error privado normalizado sin conservar cuerpos ni datos personales del API. */
export class VenueReservationsApiError extends Error {
  constructor(
    public readonly kind: VenueReservationsApiErrorKind,
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueReservationsApiError";
  }
}

/** Lista hasta cien reservas de un día usando la cookie HttpOnly del propietario. */
export async function fetchVenueReservationsForDay(
  date: string,
  signal?: AbortSignal,
): Promise<VenueReservationList> {
  const url = new URL("/api/venue/me/reservations", apiBaseUrl());
  url.searchParams.set("period", "day");
  url.searchParams.set("date", date);
  url.searchParams.set("page", "0");
  url.searchParams.set("size", "100");
  return request(url, reservationListSchema, signal);
}

/** Obtiene el detalle acreditado; nunca consulta por email ni por identificador de local. */
export async function fetchVenueReservationDetail(
  reservationId: string,
  signal?: AbortSignal,
): Promise<VenueReservationDetail> {
  const path = `/api/venue/me/reservations/${encodeURIComponent(reservationId)}`;
  return request(new URL(path, apiBaseUrl()), reservationDetailSchema, signal);
}

/** Cancela una reserva propia con un motivo que el backend persiste, audita y notifica. */
export async function cancelVenueReservation(
  reservationId: string,
  reason: string,
  signal?: AbortSignal,
): Promise<VenueReservationCancellation> {
  const path = `/api/venue/me/reservations/${encodeURIComponent(reservationId)}/cancel`;
  return request(new URL(path, apiBaseUrl()), cancellationSchema, signal, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

async function request<T>(
  url: URL,
  schema: z.ZodType<T>,
  signal?: AbortSignal,
  init?: Pick<RequestInit, "body" | "method">,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, {
      method: init?.method ?? "GET",
      body: init?.body,
      cache: "no-store",
      credentials: "include",
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
      },
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new VenueReservationsApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus: Partial<Record<number, VenueReservationsApiErrorKind>> = {
      400: "invalid",
      401: "unauthenticated",
      403: "forbidden",
      404: "notFound",
      422: "invalid",
    };
    throw new VenueReservationsApiError(byStatus[response.status] ?? "unavailable");
  }
  try {
    return schema.parse(await response.json());
  } catch (error) {
    throw new VenueReservationsApiError("unavailable", { cause: error });
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return value.endsWith("/") ? value : `${value}/`;
}
