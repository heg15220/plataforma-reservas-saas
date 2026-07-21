import { z } from "zod";
import { localizedTextSchema, reservationFormPreviewFieldSchema } from "@/features/reservation-form/reservation-form-api";

const base = () => (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/+$/, "");
const schema = z.object({ venueId: z.string().uuid(), venueSlug: z.string(), fields: z.array(reservationFormPreviewFieldSchema) });
const holdSchema = z.object({ reservationId: z.string().uuid(), holdToken: z.string(), expiresAt: z.string().datetime(), remainingSeconds: z.number().int().nonnegative() });
export type PublicReservationForm = z.infer<typeof schema>;
export type ReservationHold = z.infer<typeof holdSchema>;
export const reservationConfirmationSchema = z.object({ status: z.string(), reservationId: z.string().uuid(), manageUrlSentTo: z.string().email(), venueName: z.string(), date: z.string(), startsAt: z.string(), endsAt: z.string(), partySize: z.number().int().positive() });
export type ReservationConfirmation = z.infer<typeof reservationConfirmationSchema>;
export { localizedTextSchema };

async function json<T>(path: string, init: RequestInit, parser: z.ZodType<T>): Promise<T> {
  const response = await fetch(`${base()}${path}`, { ...init, headers: init.body ? { "Content-Type": "application/json" } : undefined });
  if (!response.ok) throw new Error(`public-reservation:${response.status}`);
  return parser.parse(await response.json());
}
export function fetchPublicReservationForm(slug: string, signal?: AbortSignal) {
  return json(`/api/public/venues/${encodeURIComponent(slug)}/reservation-form`, { method: "GET", signal }, schema);
}
export function createReservationHold(input: { venueId: string; timeSlotId: string; serviceId?: string; employeeResourceId?: string; assignmentPreference?: string; partySize: number }) {
  return json("/api/public/reservations/holds", { method: "POST", body: JSON.stringify(input) }, holdSchema);
}
export function confirmReservation(reservationId: string, input: { holdToken: string; customerName: string; customerEmail: string; partySize: number; formResponses: Array<{ fieldId: string; value: unknown }>; acceptsPrivacyPolicy: boolean; acceptsBookingRules: boolean }) {
  return json(`/api/public/reservations/${reservationId}/confirm`, { method: "POST", body: JSON.stringify(input) }, reservationConfirmationSchema);
}