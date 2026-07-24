import { z } from "zod";

const apiBase = () =>
  (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");

const managedReservationSchema = z.object({
  reservationId: z.string().uuid(),
  venueName: z.string(),
  venueAddress: z.string().nullable(),
  date: z.string(),
  startsAt: z.string(),
  endsAt: z.string(),
  partySize: z.number().int().positive(),
  status: z.string(),
  cancellable: z.boolean(),
  cancellationDeadline: z.string().datetime(),
  cancellationNoticeMinutes: z.number().int().nonnegative(),
});

const cancellationSchema = z.object({
  status: z.literal("cancelled_by_user"),
  cancelledAt: z.string().datetime(),
});

export type ManagedReservation = z.infer<typeof managedReservationSchema>;
export type ReservationCancellation = z.infer<typeof cancellationSchema>;
export type ReservationManagementErrorCode = "invalid" | "deadline" | "unavailable";

/** Error público normalizado: no conserva el token ni detalles internos del API. */
export class ReservationManagementError extends Error {
  constructor(readonly code: ReservationManagementErrorCode) {
    super(`reservation-management:${code}`);
  }
}

async function request<T>(
  token: string,
  init: RequestInit,
  schema: z.ZodType<T>,
): Promise<T> {
  const response = await fetch(
    `${apiBase()}/api/public/reservations/manage/${encodeURIComponent(token)}${init.method === "POST" ? "/cancel" : ""}`,
    init,
  );
  if (response.status === 404) throw new ReservationManagementError("invalid");
  if (response.status === 409) throw new ReservationManagementError("deadline");
  if (!response.ok) throw new ReservationManagementError("unavailable");
  return schema.parse(await response.json());
}

export function fetchManagedReservation(token: string, signal?: AbortSignal) {
  return request(token, { method: "GET", signal }, managedReservationSchema);
}

export function cancelManagedReservation(token: string) {
  return request(token, { method: "POST" }, cancellationSchema);
}
