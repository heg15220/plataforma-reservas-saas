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
    incidentsCount: z.number().int().nonnegative(),
    averageRating: z.number().min(1).max(5).nullable(),
  })
  .strict();

const demandMetricSchema = z
  .object({
    status: z.enum(["available", "insufficient_sample"]),
    policyVersion: z.literal("booking-attribution-v1"),
    definitionsVersion: z.literal("demand-commercial-metrics-v1"),
    timeZone: z.string().min(1).max(64),
    minimumSampleSize: z.number().int().positive(),
    eligibleReservations: z.number().int().nonnegative(),
    classifiedReservations: z.number().int().nonnegative(),
    coveragePercent: z.number().min(0).max(100),
    newCustomers: z.number().int().nonnegative().nullable(),
    originatedReservations: z.number().int().nonnegative().nullable(),
    offPeakCovered: z.number().int().nonnegative().nullable(),
    attributedIncome: z.number().nonnegative().nullable(),
    attributedCurrency: z
      .string()
      .regex(/^[A-Z]{3}$/)
      .nullable(),
    incomeStatus: z.enum([
      "available",
      "no_visible_price",
      "mixed_currency",
      "insufficient_sample",
    ]),
    directReservations: z.number().int().nonnegative().nullable(),
    assistedReservations: z.number().int().nonnegative().nullable(),
    generatedReservations: z.number().int().nonnegative().nullable(),
    recoveredReservations: z.number().int().nonnegative().nullable(),
    definitions: z
      .array(
        z
          .object({
            key: z.enum([
              "newCustomers",
              "originatedReservations",
              "offPeakCovered",
              "attributedIncome",
              "coverage",
            ]),
            definitionCode: z.string().regex(/^[A-Z0-9_]+$/),
          })
          .strict(),
      )
      .length(5),
  })
  .strict()
  .superRefine((value, context) => {
    const commercial = [
      value.newCustomers,
      value.originatedReservations,
      value.offPeakCovered,
      value.directReservations,
      value.assistedReservations,
      value.generatedReservations,
      value.recoveredReservations,
    ];
    if (value.status === "insufficient_sample" && commercial.some((item) => item !== null)) {
      context.addIssue({ code: "custom", message: "Insufficient sample must suppress metrics" });
    }
    if ((value.attributedIncome === null) !== (value.attributedCurrency === null)) {
      context.addIssue({ code: "custom", message: "Income and currency must be paired" });
    }
  });

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
    incidentsCount: z.number().int().nonnegative(),
    averageRating: z.number().min(1).max(5).nullable(),
    series: z.array(dailySchema).max(366),
    demandMetrics: demandMetricSchema,
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
  venueId?: string;
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
  if (filter.venueId) {
    url.searchParams.set("venueId", filter.venueId);
  }
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
