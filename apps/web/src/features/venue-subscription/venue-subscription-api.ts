import { z } from "zod";

const subscriptionStatusSchema = z.enum([
  "trial",
  "active",
  "pending_payment",
  "suspended",
  "cancelled",
]);
const paymentStatusSchema = z.enum([
  "confirmed",
  "rejected",
  "cancelled_by_user",
  "communication_error",
  "pending_confirmation",
]);
const billingPeriodSchema = z.enum(["monthly", "yearly"]);
const nullableLimit = z.number().int().nonnegative().nullable();
const planSchema = z
  .object({
    slug: z.string().regex(/^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/),
    name: z.string().trim().min(1).max(120),
    priceMonthly: z.number().nonnegative().max(1_000_000),
    priceYearly: z.number().nonnegative().max(12_000_000),
    limits: z
      .object({
        monthlyReservations: nullableLimit,
        teamResources: nullableLimit,
        customFormFields: nullableLimit,
        galleryImages: nullableLimit,
      })
      .strict(),
    features: z
      .array(
        z
          .object({
            code: z.string().regex(/^[a-z][a-z0-9_]*$/),
            label: z.string().trim().min(1).max(160),
          })
          .strict(),
      )
      .max(32),
  })
  .strict();

const monetizationSchema = z
  .object({
    status: z.enum(["disabled", "real_payments_enabled"]),
    realPaymentsEnabled: z.boolean(),
    secureExternalPaymentNoticeRequired: z.boolean(),
    provider: z.enum(["redsys"]).nullable(),
  })
  .strict()
  .superRefine((value, context) => {
    const enabled = value.status === "real_payments_enabled";
    if (
      value.realPaymentsEnabled !== enabled ||
      value.secureExternalPaymentNoticeRequired !== enabled ||
      value.provider !== (enabled ? "redsys" : null)
    ) {
      context.addIssue({ code: "custom", message: "Inconsistent monetization state" });
    }
  });

const responseSchema = z
  .object({
    currentPlan: planSchema,
    subscriptionStatus: subscriptionStatusSchema,
    billingPeriod: billingPeriodSchema,
    renewalAt: z.iso.datetime({ offset: true }).nullable(),
    trialEndsAt: z.iso.datetime({ offset: true }).nullable(),
    cancelledAt: z.iso.datetime({ offset: true }).nullable(),
    monetization: monetizationSchema,
    availablePlans: z.array(planSchema).min(1).max(20),
  })
  .strict();

const paymentHistorySchema = z
  .object({
    payments: z
      .array(
        z
          .object({
            orderReference: z.string().trim().min(1).max(128),
            amount: z.number().positive().max(1_000_000),
            currency: z.string().regex(/^[A-Z]{3}$/),
            status: paymentStatusSchema,
            createdAt: z.iso.datetime({ offset: true }),
            paidAt: z.iso.datetime({ offset: true }).nullable(),
          })
          .strict()
          .superRefine((payment, context) => {
            if ((payment.status === "confirmed") !== (payment.paidAt !== null)) {
              context.addIssue({ code: "custom", message: "Inconsistent payment date" });
            }
          }),
      )
      .max(50),
  })
  .strict();

export type VenueSubscription = z.infer<typeof responseSchema>;
export type SubscriptionPlan = z.infer<typeof planSchema>;
export type SubscriptionStatus = z.infer<typeof subscriptionStatusSchema>;
export type VenuePaymentHistory = z.infer<typeof paymentHistorySchema>;
export type VenuePayment = VenuePaymentHistory["payments"][number];
export type PaymentStatus = z.infer<typeof paymentStatusSchema>;

export class VenueSubscriptionApiError extends Error {
  constructor(
    public readonly kind: "unauthenticated" | "forbidden" | "notFound" | "unavailable",
    options?: ErrorOptions,
  ) {
    super(kind, options);
    this.name = "VenueSubscriptionApiError";
  }
}

/** Consulta la suscripción propia con cookie HttpOnly y contrato estricto sin datos financieros. */
export async function fetchVenueSubscription(signal?: AbortSignal): Promise<VenueSubscription> {
  return fetchBillingResource("/api/venue/me/subscription", responseSchema, signal);
}

/** Consulta hasta cincuenta movimientos propios sin exponer payloads o identificadores internos. */
export async function fetchVenuePaymentHistory(signal?: AbortSignal): Promise<VenuePaymentHistory> {
  return fetchBillingResource("/api/venue/me/payments", paymentHistorySchema, signal);
}

async function fetchBillingResource<T>(
  path: string,
  schema: z.ZodType<T>,
  signal?: AbortSignal,
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(new URL(path, apiBaseUrl()), {
      cache: "no-store",
      credentials: "include",
      headers: { Accept: "application/json" },
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === "AbortError") throw error;
    throw new VenueSubscriptionApiError("unavailable", { cause: error });
  }
  if (!response.ok) {
    const byStatus = {
      401: "unauthenticated",
      403: "forbidden",
      404: "notFound",
    } as const;
    throw new VenueSubscriptionApiError(
      byStatus[response.status as keyof typeof byStatus] ?? "unavailable",
    );
  }
  try {
    return schema.parse(await response.json());
  } catch (error) {
    throw new VenueSubscriptionApiError("unavailable", { cause: error });
  }
}

function apiBaseUrl() {
  const value = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
  return value.endsWith("/") ? value : `${value}/`;
}
