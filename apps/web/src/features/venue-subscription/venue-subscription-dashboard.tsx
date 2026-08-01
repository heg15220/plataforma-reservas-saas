"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Chip from "@mui/material/Chip";
import CircularProgress from "@mui/material/CircularProgress";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Check, CreditCard, History, ShieldCheck, Sparkles } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useMemo, useState } from "react";

import { Surface } from "@/components/layout";

import {
  fetchVenuePaymentHistory,
  fetchVenueSubscription,
  type PaymentStatus,
  type SubscriptionPlan,
  type SubscriptionStatus,
  type VenuePayment,
  type VenuePaymentHistory,
  type VenueSubscription,
  VenueSubscriptionApiError,
} from "./venue-subscription-api";

const STATUS_TONES = {
  trial: "info",
  active: "success",
  pending_payment: "warning",
  suspended: "error",
  cancelled: "default",
} as const satisfies Record<
  SubscriptionStatus,
  "default" | "error" | "info" | "success" | "warning"
>;

const PAYMENT_STATUS_TONES = {
  confirmed: "success",
  rejected: "error",
  cancelled_by_user: "default",
  communication_error: "warning",
  pending_confirmation: "warning",
} as const satisfies Record<PaymentStatus, "default" | "error" | "success" | "warning">;

/** Panel responsive de plan actual, monetización y catálogo disponible. */
export function VenueSubscriptionDashboard() {
  const t = useTranslations("VenueSubscription");
  const locale = useLocale();
  const [data, setData] = useState<VenueSubscription | null>(null);
  const [history, setHistory] = useState<VenuePaymentHistory | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<VenueSubscriptionApiError["kind"] | null>(null);
  const currency = useMemo(
    () => new Intl.NumberFormat(locale, { style: "currency", currency: "EUR" }),
    [locale],
  );

  useEffect(() => {
    const controller = new AbortController();
    Promise.all([
      fetchVenueSubscription(controller.signal),
      fetchVenuePaymentHistory(controller.signal),
    ])
      .then(([subscription, payments]) => {
        if (!controller.signal.aborted) {
          setData(subscription);
          setHistory(payments);
        }
      })
      .catch((reason: unknown) => {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
        setError(reason instanceof VenueSubscriptionApiError ? reason.kind : "unavailable");
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  if (loading) {
    return (
      <Stack
        aria-label={t("loading")}
        role="status"
        sx={{ alignItems: "center", justifyContent: "center", minHeight: 320 }}
      >
        <CircularProgress size={36} />
      </Stack>
    );
  }
  if (error || !data || !history) {
    return (
      <Alert severity="error" sx={{ mt: 6 }}>
        {t(`errors.${error ?? "unavailable"}`)}
      </Alert>
    );
  }

  return (
    <Stack spacing={4} sx={{ mt: { xs: 4, sm: 6 }, minWidth: 0 }}>
      <Surface component="section">
        <Stack
          direction="column"
          spacing={3}
          sx={{ alignItems: "flex-start", justifyContent: "space-between" }}
        >
          <Box sx={{ minWidth: 0 }}>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start", minWidth: 0 }}>
              <CreditCard aria-hidden="true" size={22} style={{ flexShrink: 0 }} />
              <Typography component="h2" sx={{ overflowWrap: "anywhere" }} variant="h2">
                {t("current.title")}
              </Typography>
            </Stack>
            <Typography
              component="p"
              sx={{ fontWeight: 800, mt: 3, overflowWrap: "anywhere" }}
              variant="h1"
            >
              {data.currentPlan.name}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              {t(`billingPeriod.${data.billingPeriod}`)}
            </Typography>
          </Box>
          <Chip
            color={STATUS_TONES[data.subscriptionStatus]}
            label={t(`status.${data.subscriptionStatus}.label`)}
            sx={responsiveChipSx}
          />
        </Stack>
        <Typography color="text.secondary" sx={{ mt: 3 }}>
          {t(`status.${data.subscriptionStatus}.description`)}
        </Typography>
        <Box
          component="dl"
          sx={{
            display: "grid",
            gap: 2,
            gridTemplateColumns: { sm: "repeat(2, minmax(0, 1fr))" },
            m: 0,
            mt: 4,
          }}
        >
          <Detail
            label={t("current.monthlyPrice")}
            value={currency.format(data.currentPlan.priceMonthly)}
          />
          <Detail
            label={t("current.yearlyPrice")}
            value={currency.format(data.currentPlan.priceYearly)}
          />
          <Detail
            label={t("current.renewal")}
            value={data.renewalAt ? formatDate(data.renewalAt, locale) : t("current.notApplicable")}
          />
          <Detail
            label={t("current.trialEnd")}
            value={
              data.trialEndsAt ? formatDate(data.trialEndsAt, locale) : t("current.notApplicable")
            }
          />
        </Box>
        <Divider sx={{ my: 4 }} />
        <Typography component="h3" variant="h3">
          {t("current.features")}
        </Typography>
        <FeatureList features={data.currentPlan.features} />
      </Surface>

      {data.monetization.realPaymentsEnabled &&
        data.monetization.secureExternalPaymentNoticeRequired && (
          <Alert icon={<ShieldCheck aria-hidden="true" />} severity="warning">
            <Typography sx={{ fontWeight: 800 }}>{t("monetization.redsys.title")}</Typography>
            <Typography sx={{ mt: 1 }}>{t("monetization.redsys.description")}</Typography>
          </Alert>
        )}
      {!data.monetization.realPaymentsEnabled && (
        <Alert severity="info">
          <Typography sx={{ fontWeight: 800 }}>{t("monetization.disabled.title")}</Typography>
          <Typography sx={{ mt: 1 }}>{t("monetization.disabled.description")}</Typography>
        </Alert>
      )}

      <Box component="section" aria-labelledby="subscription-plans-title">
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start", minWidth: 0 }}>
          <Sparkles aria-hidden="true" size={22} style={{ flexShrink: 0 }} />
          <Typography
            component="h2"
            id="subscription-plans-title"
            sx={{ overflowWrap: "anywhere" }}
            variant="h2"
          >
            {t("plans.title")}
          </Typography>
        </Stack>
        <Typography color="text.secondary" sx={{ mt: 1 }}>
          {t("plans.description")}
        </Typography>
        <Box
          sx={{
            display: "grid",
            gap: 3,
            gridTemplateColumns: {
              xs: "minmax(0, 1fr)",
              md: "repeat(2, minmax(0, 1fr))",
              xl: "repeat(3, minmax(0, 1fr))",
            },
            mt: 4,
          }}
        >
          {data.availablePlans.map((plan) => (
            <PlanCard
              currency={currency}
              current={plan.slug === data.currentPlan.slug}
              key={plan.slug}
              plan={plan}
            />
          ))}
        </Box>
      </Box>

      <Surface component="section">
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "flex-start", minWidth: 0 }}>
          <History aria-hidden="true" size={22} style={{ flexShrink: 0 }} />
          <Typography component="h2" sx={{ overflowWrap: "anywhere" }} variant="h2">
            {t("history.title")}
          </Typography>
        </Stack>
        {history.payments.length === 0 ? (
          <Typography color="text.secondary" sx={{ mt: 2 }}>
            {t("history.empty")}
          </Typography>
        ) : (
          <Stack component="ul" spacing={2} sx={{ listStyle: "none", m: 0, mt: 3, p: 0 }}>
            {history.payments.map((payment) => (
              <PaymentHistoryItem
                key={`${payment.orderReference}-${payment.createdAt}`}
                locale={locale}
                payment={payment}
              />
            ))}
          </Stack>
        )}
      </Surface>
    </Stack>
  );
}

function PaymentHistoryItem({ locale, payment }: { locale: string; payment: VenuePayment }) {
  const t = useTranslations("VenueSubscription");
  const currency = new Intl.NumberFormat(locale, {
    style: "currency",
    currency: payment.currency,
  });
  return (
    <Box
      component="li"
      sx={{
        border: 1,
        borderColor: "divider",
        borderRadius: 3,
        display: "grid",
        gap: 2,
        gridTemplateColumns: {
          lg: "minmax(0, 1fr) minmax(6rem, auto) auto minmax(0, 0.8fr)",
        },
        p: 2.5,
      }}
    >
      <Box sx={{ minWidth: 0 }}>
        <Typography sx={{ fontWeight: 800, overflowWrap: "anywhere" }}>
          {payment.orderReference}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
          {t("history.createdAt", { date: formatDate(payment.createdAt, locale) })}
        </Typography>
      </Box>
      <Typography sx={{ alignSelf: "center", fontWeight: 800 }}>
        {currency.format(payment.amount)}
      </Typography>
      <Chip
        color={PAYMENT_STATUS_TONES[payment.status]}
        label={t(`history.status.${payment.status}`)}
        size="small"
        sx={{ ...responsiveChipSx, alignSelf: "center", justifySelf: "start" }}
      />
      <Typography
        color="text.secondary"
        sx={{ alignSelf: "center", overflowWrap: "anywhere" }}
        variant="body2"
      >
        {payment.paidAt
          ? t("history.paidAt", { date: formatDate(payment.paidAt, locale) })
          : t("history.notPaid")}
      </Typography>
    </Box>
  );
}

function PlanCard({
  currency,
  current,
  plan,
}: {
  currency: Intl.NumberFormat;
  current: boolean;
  plan: SubscriptionPlan;
}) {
  const t = useTranslations("VenueSubscription");
  return (
    <Surface component="article">
      <Stack
        direction="column"
        spacing={2}
        sx={{ alignItems: "flex-start", justifyContent: "space-between" }}
      >
        <Typography component="h3" sx={{ overflowWrap: "anywhere" }} variant="h2">
          {plan.name}
        </Typography>
        {current && (
          <Chip color="primary" label={t("plans.current")} size="small" sx={responsiveChipSx} />
        )}
      </Stack>
      <Typography sx={{ fontWeight: 800, mt: 3 }} variant="h2">
        {t("plans.monthlyPrice", { price: currency.format(plan.priceMonthly) })}
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1 }}>
        {t("plans.yearlyPrice", { price: currency.format(plan.priceYearly) })}
      </Typography>
      <Divider sx={{ my: 3 }} />
      <FeatureList features={plan.features} />
      <Divider sx={{ my: 3 }} />
      <Stack spacing={1}>
        <Limit label={t("limits.monthlyReservations")} value={plan.limits.monthlyReservations} />
        <Limit label={t("limits.teamResources")} value={plan.limits.teamResources} />
        <Limit label={t("limits.customFormFields")} value={plan.limits.customFormFields} />
        <Limit label={t("limits.galleryImages")} value={plan.limits.galleryImages} />
      </Stack>
    </Surface>
  );
}

function FeatureList({ features }: { features: SubscriptionPlan["features"] }) {
  return (
    <Stack component="ul" spacing={1.5} sx={{ listStyle: "none", m: 0, mt: 2, p: 0 }}>
      {features.map((feature) => (
        <Stack
          component="li"
          direction="row"
          key={feature.code}
          spacing={1.5}
          sx={{ alignItems: "flex-start" }}
        >
          <Check aria-hidden="true" color="currentColor" size={18} style={{ flexShrink: 0 }} />
          <Typography sx={{ minWidth: 0, overflowWrap: "anywhere" }}>{feature.label}</Typography>
        </Stack>
      ))}
    </Stack>
  );
}

function Limit({ label, value }: { label: string; value: number | null }) {
  const t = useTranslations("VenueSubscription");
  return (
    <Typography color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
      {t("limits.value", { label, value: value ?? t("limits.unlimited") })}
    </Typography>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ borderBottom: 1, borderColor: "divider", pb: 2 }}>
      <Typography component="dt" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>
        {label}
      </Typography>
      <Typography component="dd" sx={{ fontWeight: 800, m: 0, mt: 1, overflowWrap: "anywhere" }}>
        {value}
      </Typography>
    </Box>
  );
}

function formatDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(new Date(value));
}

/** Permite que estados traducidos largos se ajusten sin ampliar tarjetas o paneles. */
const responsiveChipSx = {
  height: "auto",
  maxWidth: "100%",
  "& .MuiChip-label": {
    display: "block",
    overflowWrap: "anywhere",
    py: 0.5,
    whiteSpace: "normal",
  },
};
