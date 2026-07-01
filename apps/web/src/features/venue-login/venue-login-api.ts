import { z } from "zod";

import { supportedLocales } from "@/i18n/config";

import type { VenueLoginPayload } from "./venue-login-schema";

const venueLoginResultSchema = z.object({
  userId: z.uuid(),
  accountType: z.literal("venue_business"),
  preferredLocale: z.enum(supportedLocales),
  emailVerified: z.boolean(),
  sessionExpiresAt: z.iso.datetime({ offset: true }),
});

export type VenueLoginResult = z.infer<typeof venueLoginResultSchema>;
export type VenueLoginErrorKind = "invalid" | "rateLimited" | "unavailable";

export class VenueLoginApiError extends Error {
  constructor(
    public readonly kind: VenueLoginErrorKind,
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueLoginApiError";
  }
}

/**
 * Autentica al propietario y deja que el navegador reciba la cookie HttpOnly.
 *
 * El cliente valida solo metadatos no sensibles. No lee, persiste ni registra
 * el secreto de sesión, y reduce cualquier fallo a categorías seguras de UI.
 */
export async function loginVenue(
  payload: VenueLoginPayload,
  signal?: AbortSignal,
): Promise<VenueLoginResult> {
  let response: Response;

  try {
    response = await fetch(`${readPublicApiBaseUrl().replace(/\/$/, "")}/api/auth/login`, {
      method: "POST",
      credentials: "include",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
      signal,
    });
  } catch (error) {
    throw new VenueLoginApiError("unavailable", { cause: error });
  }

  if (response.status === 400 || response.status === 401) {
    throw new VenueLoginApiError("invalid");
  }
  if (response.status === 429) {
    throw new VenueLoginApiError("rateLimited");
  }
  if (!response.ok) {
    throw new VenueLoginApiError("unavailable");
  }

  try {
    return venueLoginResultSchema.parse(await response.json());
  } catch (error) {
    throw new VenueLoginApiError("unavailable", { cause: error });
  }
}

function readPublicApiBaseUrl(): string {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL;

  if (!value) {
    throw new VenueLoginApiError("unavailable");
  }

  return value;
}
