import { z } from "zod";

const periodSchema = z.enum(["today", "week", "month", "year", "custom"]);
const isoDateSchema = z.iso.date();

const dailySchema = z
  .object({
    date: isoDateSchema,
    reservationsCount: z.number().int().nonnegative(),
    confirmedCount: z.number().int().nonnegative(),
    cancelledCount: z.number().int().nonnegative(),
    noShowCount: z.number().int().nonnegative(),
    attendedCount: z.number().int().nonnegative(),
    occupiedCapacity: z.number().int().nonnegative(),
    availableCapacity: z.number().int().nonnegative(),
    occupancyRate: z.number().nonnegative(),
    reviewsCount: z.number().int().nonnegative(),
    averageRating: z.number().min(1).max(5).nullable(),
  })
  .strict();

const statisticsSchema = z
  .object({
    period: periodSchema,
    fromDate: isoDateSchema,
    toDate: isoDateSchema,
    reservationsCount: z.number().int().nonnegative(),
    confirmedCount: z.number().int().nonnegative(),
    cancelledCount: z.number().int().nonnegative(),
    noShowCount: z.number().int().nonnegative(),
    attendedCount: z.number().int().nonnegative(),
    occupiedCapacity: z.number().int().nonnegative(),
    availableCapacity: z.number().int().nonnegative(),
    occupancyRate: z.number().nonnegative(),
    reviewsCount: z.number().int().nonnegative(),
    averageRating: z.number().min(1).max(5).nullable(),
    series: z.array(dailySchema).max(366),
  })
  .strict()
  .superRefine((value, context) => {
    if (value.fromDate > value.toDate) {
      context.addIssue({ code: "custom", message: "Invalid date range" });
    }
    if (value.period === "custom" && value.series.length > 366) {
      context.addIssue({ code: "custom", message: "Range is too large" });
    }
  });

export type VenueStatistics = z.infer<typeof statisticsSchema>;
export type VenueStatisticsPeriod = z.infer<typeof periodSchema>;

export interface VenueStatisticsFilter {
  period: VenueStatisticsPeriod;
  from?: string;
  to?: string;
}

export class VenueStatisticsApiError extends Error {
  constructor(
    public readonly kind: "unauthenticated" | "forbidden" | "notFound" | "invalid" | "unavailable",
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueStatisticsApiError";
  }
}

/** Consulta métricas propias con cookie HttpOnly y valida estrictamente la respuesta. */
export async function fetchVenueStatistics(
  filter: VenueStatisticsFilter,
  signal?: AbortSignal,
): Promise<VenueStatistics> {
  const url = new URL("/api/venue/me/statistics", apiBaseUrl());
  url.searchParams.set("period", filter.period);
  if (filter.period === "custom") {
    if (!filter.from || !filter.to) {
      throw new VenueStatisticsApiError("invalid");
    }
    url.searchParams.set("from", filter.from);
    url.searchParams.set("to", filter.to);
  }

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
    throw new VenueStatisticsApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus = {
      400: "invalid",
      401: "unauthenticated",
      403: "forbidden",
      404: "notFound",
    } as const;
    throw new VenueStatisticsApiError(
      byStatus[response.status as keyof typeof byStatus] ?? "unavailable",
    );
  }
  try {
    return statisticsSchema.parse(await response.json());
  } catch (error) {
    throw new VenueStatisticsApiError("unavailable", { cause: error });
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return value.endsWith("/") ? value : `${value}/`;
}
