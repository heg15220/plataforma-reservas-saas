import { z } from "zod";

const eligibilitySchema = z.discriminatedUnion("eligible", [
  z
    .object({
      eligible: z.literal(true),
      canReview: z.literal(true),
      error: z.null(),
      messageKey: z.null(),
    })
    .strict(),
  z
    .object({
      eligible: z.literal(false),
      canReview: z.literal(false),
      error: z.enum(["REVIEW_NOT_ELIGIBLE", "REVIEW_ALREADY_SUBMITTED"]),
      messageKey: z.enum([
        "reviews.notEligibleForVenue",
        "reviews.alreadySubmittedForVenue",
      ]),
    })
    .strict(),
]);

const createdReviewSchema = z
  .object({
    status: z.literal("created"),
    reviewId: z.uuid(),
    venueId: z.uuid(),
    rating: z.number().int().min(1).max(5),
    averageRating: z.number().min(1).max(5),
    reviewsCount: z.number().int().positive(),
  })
  .strict();

export type ReviewEligibility = z.infer<typeof eligibilitySchema>;
export type CreatedPublicReview = z.infer<typeof createdReviewSchema>;

export interface PublicReviewCreateCommand {
  customerEmail: string;
  rating: number;
  comment: string | null;
  acceptsReviewPolicy: boolean;
}

export class PublicReviewApiError extends Error {
  constructor(
    public readonly kind:
      | "invalid"
      | "notEligible"
      | "alreadySubmitted"
      | "unavailable",
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "PublicReviewApiError";
  }
}

/** Comprueba elegibilidad sin recibir ningún dato de reservas o historial del email. */
export async function checkPublicReviewEligibility(
  venueSlug: string,
  customerEmail: string,
  signal?: AbortSignal,
): Promise<ReviewEligibility> {
  const response = await request(
    `/api/public/venues/${encodeURIComponent(venueSlug)}/reviews/eligibility`,
    { customerEmail },
    signal,
  );
  try {
    return eligibilitySchema.parse(await response.json());
  } catch (error) {
    throw new PublicReviewApiError("unavailable", { cause: error });
  }
}

/** Crea desde la ficha; backend vuelve a elegir y bloquear una reserva elegible. */
export async function createPublicVenueReview(
  venueSlug: string,
  command: PublicReviewCreateCommand,
  signal?: AbortSignal,
): Promise<CreatedPublicReview> {
  const response = await request(
    `/api/public/venues/${encodeURIComponent(venueSlug)}/reviews`,
    command,
    signal,
  );
  try {
    return createdReviewSchema.parse(await response.json());
  } catch (error) {
    throw new PublicReviewApiError("unavailable", { cause: error });
  }
}

async function request(path: string, body: unknown, signal?: AbortSignal) {
  let response: Response;
  try {
    response = await fetch(new URL(path, apiBaseUrl()), {
      body: JSON.stringify(body),
      cache: "no-store",
      credentials: "include",
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
      },
      method: "POST",
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new PublicReviewApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus = {
      400: "invalid",
      409: "alreadySubmitted",
      422: "notEligible",
    } as const;
    throw new PublicReviewApiError(
      byStatus[response.status as keyof typeof byStatus] ?? "unavailable",
    );
  }
  return response;
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return value.endsWith("/") ? value : `${value}/`;
}
