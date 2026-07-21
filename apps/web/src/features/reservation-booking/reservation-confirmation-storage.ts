import { z } from "zod";

const reservationConfirmationSchema = z.object({
  status: z.literal("confirmed"),
  reservationId: z.string().uuid(),
  manageUrlSentTo: z.string().email(),
  venueName: z.string().min(1),
  date: z.iso.date(),
  startsAt: z.iso.time(),
  endsAt: z.iso.time(),
  partySize: z.number().int().positive(),
});

export type ReservationConfirmation = z.infer<typeof reservationConfirmationSchema>;

/** Guarda el snapshot confirmado en la sesión sin exponer email ni detalles en la URL. */
export function storeReservationConfirmation(value: ReservationConfirmation): void {
  const confirmation = reservationConfirmationSchema.parse(value);
  window.sessionStorage.setItem(storageKey(confirmation.reservationId), JSON.stringify(confirmation));
}

/** Recupera un snapshot confirmado y rechaza contenido manipulado o de otra reserva. */
export function readReservationConfirmation(reservationId: string): ReservationConfirmation | null {
  const stored = window.sessionStorage.getItem(storageKey(reservationId));
  if (!stored) return null;
  try {
    const parsed = reservationConfirmationSchema.safeParse(JSON.parse(stored));
    return parsed.success && parsed.data.reservationId === reservationId ? parsed.data : null;
  } catch {
    return null;
  }
}

function storageKey(reservationId: string): string {
  return `reserly:reservation-confirmation:${reservationId}`;
}