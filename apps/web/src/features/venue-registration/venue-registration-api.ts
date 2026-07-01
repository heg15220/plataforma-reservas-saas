import { z } from "zod";

import { businessVerificationStatuses } from "@/features/verification/verification-status";

import type { VenueRegistrationPayload } from "./venue-registration-schema";

const venueRegistrationResultSchema = z.object({
  accountType: z.literal("venue_business"),
  businessVerificationStatus: z.enum(businessVerificationStatuses),
  emailVerificationRequired: z.boolean(),
  canPublishVenue: z.literal(false),
});

export type VenueRegistrationResult = z.infer<typeof venueRegistrationResultSchema>;

export class VenueRegistrationApiError extends Error {
  constructor(
    public readonly kind: "conflict" | "invalid" | "rateLimited" | "unavailable",
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueRegistrationApiError";
  }
}

/**
 * Envía el alta empresarial sin persistir ni registrar credenciales en cliente.
 *
 * Los errores públicos se reducen a categorías aptas para UI. No se propagan
 * payloads, mensajes internos ni pistas sobre el campo que causó un conflicto.
 */
export async function registerVenue(
  payload: VenueRegistrationPayload,
  signal?: AbortSignal,
): Promise<VenueRegistrationResult> {
  let response: Response;

  try {
    response = await fetch(
      `${readPublicApiBaseUrl().replace(/\/$/, "")}/api/auth/venues/register`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
        signal,
      },
    );
  } catch (error) {
    throw new VenueRegistrationApiError("unavailable", { cause: error });
  }

  if (response.status === 409) {
    throw new VenueRegistrationApiError("conflict");
  }
  if (response.status === 400) {
    throw new VenueRegistrationApiError("invalid");
  }
  if (response.status === 429) {
    throw new VenueRegistrationApiError("rateLimited");
  }
  if (!response.ok) {
    throw new VenueRegistrationApiError("unavailable");
  }

  try {
    return venueRegistrationResultSchema.parse(await response.json());
  } catch (error) {
    throw new VenueRegistrationApiError("unavailable", { cause: error });
  }
}

function readPublicApiBaseUrl(): string {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL;

  if (!value) {
    throw new VenueRegistrationApiError("unavailable");
  }

  return value;
}
