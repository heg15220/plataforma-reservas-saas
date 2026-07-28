import { z } from "zod";

const reviewPageSchema = z.object({
  averageRating: z.number().min(1).max(5).nullable(),
  reviewsCount: z.number().int().nonnegative(),
  items: z.array(
    z.object({
      id: z.uuid(),
      rating: z.number().int().min(1).max(5),
      comment: z.string().nullable(),
      createdAt: z.string().datetime(),
    }),
  ),
  page: z.number().int().nonnegative(),
  size: z.number().int().min(1).max(100),
  totalPages: z.number().int().nonnegative(),
});

export type VenueReviewPage = z.infer<typeof reviewPageSchema>;

export class VenueReviewsApiError extends Error {
  constructor(
    public readonly kind:
      | "unauthenticated"
      | "forbidden"
      | "notFound"
      | "invalid"
      | "unavailable",
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueReviewsApiError";
  }
}

/** Consulta una página acotada de reseñas del local derivado de la cookie de sesión. */
export async function fetchVenueReviews(
  page: number,
  signal?: AbortSignal,
): Promise<VenueReviewPage> {
  const url = new URL("/api/venue/me/reviews", apiBaseUrl());
  url.searchParams.set("page", String(page));
  url.searchParams.set("size", "20");
  let response: Response;
  try {
    response = await fetch(url, {
      cache: "no-store",
      credentials: "include",
      headers: { Accept: "application/json" },
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new VenueReviewsApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus = {
      400: "invalid",
      401: "unauthenticated",
      403: "forbidden",
      404: "notFound",
    } as const;
    throw new VenueReviewsApiError(
      byStatus[response.status as keyof typeof byStatus] ?? "unavailable",
    );
  }
  try {
    return reviewPageSchema.parse(await response.json());
  } catch (error) {
    throw new VenueReviewsApiError("unavailable", { cause: error });
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return value.endsWith("/") ? value : `${value}/`;
}
