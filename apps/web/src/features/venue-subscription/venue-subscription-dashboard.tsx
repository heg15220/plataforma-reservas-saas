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
  fetchVenueSubscription,
  type SubscriptionPlan,
  type SubscriptionStatus,
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

/** Panel responsive de plan actual, monetización y catálogo disponible. */
export function VenueSubscriptionDashboard() {
  const t = useTranslations("VenueSubscription");
  const locale = useLocale();
  const [data, setData] = useState<VenueSubscription | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<VenueSubscriptionApiError["kind"] | null>(null);
  const currency = useMemo(
    () => new Intl.NumberFormat(locale, { style: "currency", currency: "EUR" }),
    [locale],
  );

  useEffect(() => {
    const controller = new AbortController();
    fetchVenueSubscription(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
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
  if (error || !data) {
    return (
      <Alert severity="error" sx={{ mt: 6 }}>
        {t(`errors.${error ?? "unavailable"}`)}
      </Alert>
    );
  }

  return (
    <Stack spacing={4} sx={{ mt: 6 }}>
      <Surface component="section">
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={3}
          sx={{ alignItems: { sm: "flex-start" }, justifyContent: "space-between" }}
        >
          <Box>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
              <CreditCard aria-hidden="true" size={22} />
              <Typography component="h2" variant="h2">
                {t("current.title")}
              </Typography>
            </Stack>
            <Typography component="p" sx={{ fontWeight: 800, mt: 3 }} variant="h1">
              {data.currentPlan.name}
            </Typography>
            <Typography color="text.secondary" sx={{ mt: 1 }}>
              {t(`billingPeriod.${data.billingPeriod}`)}
            </Typography>
          </Box>
          <Chip
            color={STATUS_TONES[data.subscriptionStatus]}
            label={t(`status.${data.subscriptionStatus}.label`)}
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
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <Sparkles aria-hidden="true" size={22} />
          <Typography component="h2" id="subscription-plans-title" variant="h2">
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
            gridTemplateColumns: { xs: "1fr", lg: "repeat(3, minmax(0, 1fr))" },
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
        <Stack direction="row" spacing={1.5} sx={{ alignItems: "center" }}>
          <History aria-hidden="true" size={22} />
          <Typography component="h2" variant="h2">
            {t("history.title")}
          </Typography>
        </Stack>
        <Typography color="text.secondary" sx={{ mt: 2 }}>
          {t("history.empty")}
        </Typography>
      </Surface>
    </Stack>
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
        direction="row"
        spacing={2}
        sx={{ alignItems: "center", justifyContent: "space-between" }}
      >
        <Typography component="h3" variant="h2">
          {plan.name}
        </Typography>
        {current && <Chip color="primary" label={t("plans.current")} size="small" />}
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
          <Check aria-hidden="true" color="currentColor" size={18} />
          <Typography>{feature.label}</Typography>
        </Stack>
      ))}
    </Stack>
  );
}

function Limit({ label, value }: { label: string; value: number | null }) {
  const t = useTranslations("VenueSubscription");
  return (
    <Typography color="text.secondary">
      {t("limits.value", { label, value: value ?? t("limits.unlimited") })}
    </Typography>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ borderBottom: 1, borderColor: "divider", pb: 2 }}>
      <Typography component="dt" color="text.secondary">
        {label}
      </Typography>
      <Typography component="dd" sx={{ fontWeight: 800, m: 0, mt: 1 }}>
        {value}
      </Typography>
    </Box>
  );
}

function formatDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, { dateStyle: "medium" }).format(new Date(value));
}
