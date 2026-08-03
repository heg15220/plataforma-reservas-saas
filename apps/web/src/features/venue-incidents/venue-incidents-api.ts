import { z } from "zod";

const instantSchema = z.string().datetime();

const bookingRulesSchema = z.object({
  cancellationAllowed: z.boolean(),
  freeCancellationUntilMinutesBefore: z.number().int().min(0).max(525_600),
  updatedAt: instantSchema,
});

const incidentHistorySchema = z.object({
  page: z.number().int().nonnegative(),
  size: z.number().int().positive().max(50),
  totalElements: z.number().int().nonnegative(),
  totalPages: z.number().int().nonnegative(),
  items: z.array(
    z.object({
      incidentType: z.string().min(1),
      reportedAt: instantSchema,
      status: z.string().min(1),
    }),
  ),
});

const attendanceSchema = z.object({
  reservationId: z.uuid(),
  status: z.string().min(1),
  attendanceMarkedAt: instantSchema,
  updatedAt: instantSchema,
});

const reportSchema = z.object({
  incidentId: z.uuid(),
  reservationId: z.uuid(),
  status: z.string().min(1),
  reportedAt: instantSchema,
});

export type VenueBookingRules = z.infer<typeof bookingRulesSchema>;
export type VenueIncidentHistory = z.infer<typeof incidentHistorySchema>;
export type AttendanceStatus = "attended" | "no_show";

export class VenueIncidentsApiError extends Error {
  constructor(
    public readonly kind:
      | "unauthenticated"
      | "forbidden"
      | "notFound"
      | "invalid"
      | "conflict"
      | "unavailable",
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueIncidentsApiError";
  }
}

/** Carga la configuración de cancelación del local autenticado. */
export function fetchVenueBookingRules(signal?: AbortSignal) {
  return request(new URL("/api/venue/me/booking-rules", apiBaseUrl()), bookingRulesSchema, signal);
}

/** Reemplaza las reglas básicas con límites idénticos al contrato backend. */
export function updateVenueBookingRules(
  values: Pick<VenueBookingRules, "cancellationAllowed" | "freeCancellationUntilMinutesBefore">,
  signal?: AbortSignal,
) {
  return request(new URL("/api/venue/me/booking-rules", apiBaseUrl()), bookingRulesSchema, signal, {
    method: "PUT",
    body: JSON.stringify(values),
  });
}

/** Consulta historial usando una reserva propia como acreditación, nunca un email libre. */
export function fetchVenueIncidentHistory(reservationId: string, signal?: AbortSignal) {
  const url = new URL("/api/venue/me/incident-history", apiBaseUrl());
  url.searchParams.set("reservationId", reservationId);
  url.searchParams.set("page", "0");
  url.searchParams.set("size", "50");
  return request(url, incidentHistorySchema, signal);
}

/** Marca asistencia mediante botones táctiles del detalle privado. */
export function updateReservationAttendance(
  reservationId: string,
  status: AttendanceStatus,
  signal?: AbortSignal,
) {
  const path = `/api/venue/me/reservations/${encodeURIComponent(reservationId)}/attendance`;
  return request(new URL(path, apiBaseUrl()), attendanceSchema, signal, {
    method: "POST",
    body: JSON.stringify({ status }),
  });
}

/** Confirma el reporte auditado después de la advertencia visible. */
export function reportReservationNoShow(
  reservationId: string,
  notes: string,
  signal?: AbortSignal,
) {
  const path = `/api/venue/me/reservations/${encodeURIComponent(reservationId)}/report-no-show`;
  return request(new URL(path, apiBaseUrl()), reportSchema, signal, {
    method: "POST",
    body: JSON.stringify({ confirmed: true, notes: notes.trim() || null }),
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
    throw new VenueIncidentsApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus = {
      400: "invalid",
      401: "unauthenticated",
      403: "forbidden",
      404: "notFound",
      409: "conflict",
      422: "invalid",
    } as const;
    throw new VenueIncidentsApiError(
      byStatus[response.status as keyof typeof byStatus] ?? "unavailable",
    );
  }
  try {
    return schema.parse(await response.json());
  } catch (error) {
    throw new VenueIncidentsApiError("unavailable", { cause: error });
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return value.endsWith("/") ? value : `${value}/`;
}
