import { z } from "zod";

import {
  localizedTextSchema,
  reservationFormPreviewFieldSchema,
} from "@/features/reservation-form/reservation-form-api";
import { demandCorrelationHeaders } from "@/features/demand-telemetry/demand-correlation";

const publicReservationFormSchema = z.object({
  venueId: z.uuid(),
  venueSlug: z.string(),
  fields: z.array(reservationFormPreviewFieldSchema),
});

const reservationHoldSchema = z.object({
  reservationId: z.uuid(),
  holdToken: z.string(),
  expiresAt: z.iso.datetime(),
  remainingSeconds: z.number().int().nonnegative(),
});

export const reservationConfirmationSchema = z.object({
  status: z.literal("confirmed"),
  reservationId: z.uuid(),
  manageUrlSentTo: z.email(),
  venueName: z.string(),
  date: z.string(),
  startsAt: z.string(),
  endsAt: z.string(),
  partySize: z.number().int().positive(),
});

const activeRestrictionSchema = z.object({
  error: z.literal("ACTIVE_BOOKING_RESTRICTION"),
  restrictedUntil: z.iso.date(),
});

export type PublicReservationForm = z.infer<typeof publicReservationFormSchema>;
export type ReservationHold = z.infer<typeof reservationHoldSchema>;
export type ReservationConfirmation = z.infer<typeof reservationConfirmationSchema>;
export { localizedTextSchema };

/** Error público minimizado; solo la restricción activa expone su fecha de finalización. */
export class PublicReservationApiError extends Error {
  constructor(
    readonly kind: "activeRestriction" | "unavailable",
    readonly restrictedUntil?: string,
  ) {
    super(`public-reservation:${kind}`);
    this.name = "PublicReservationApiError";
  }
}

function apiBaseUrl() {
  return (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");
}

/**
 * Ejecuta un contrato público sin reflejar detalles internos del backend.
 *
 * El único error de dominio representable conserva la fecha necesaria para localizar el mensaje
 * profesional de restricción. Cualquier otro cuerpo se reduce a indisponibilidad genérica.
 */
async function request<T>(path: string, init: RequestInit, parser: z.ZodType<T>): Promise<T> {
  const response = await fetch(`${apiBaseUrl()}${path}`, {
    ...init,
    headers: {
      ...demandCorrelationHeaders(),
      ...(init.body ? { "Content-Type": "application/json" } : {}),
    },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    const restriction = activeRestrictionSchema.safeParse(body);
    if (response.status === 409 && restriction.success) {
      throw new PublicReservationApiError("activeRestriction", restriction.data.restrictedUntil);
    }
    throw new PublicReservationApiError("unavailable");
  }
  return parser.parse(await response.json());
}

/** Obtiene exclusivamente el formulario publicado del local identificado por slug. */
export function fetchPublicReservationForm(slug: string, signal?: AbortSignal) {
  return request(
    `/api/public/venues/${encodeURIComponent(slug)}/reservation-form`,
    { method: "GET", signal },
    publicReservationFormSchema,
  );
}

/** Crea un hold con la selección pública exacta; disponibilidad se revalida en backend. */
export function createReservationHold(input: {
  venueId: string;
  timeSlotId: string;
  serviceId?: string;
  employeeResourceId?: string;
  assignmentPreference?: string;
  partySize: number;
}) {
  return request(
    "/api/public/reservations/holds",
    { method: "POST", body: JSON.stringify(input) },
    reservationHoldSchema,
  );
}

/** Confirma el hold o devuelve una restricción temporal minimizada y localizable. */
export function confirmReservation(
  reservationId: string,
  input: {
    holdToken: string;
    customerName: string;
    customerEmail: string;
    locale: "es" | "en";
    partySize: number;
    formResponses: Array<{ fieldId: string; value: unknown }>;
    acceptsPrivacyPolicy: boolean;
    acceptsBookingRules: boolean;
  },
) {
  return request(
    `/api/public/reservations/${reservationId}/confirm`,
    { method: "POST", body: JSON.stringify(input) },
    reservationConfirmationSchema,
  );
}
