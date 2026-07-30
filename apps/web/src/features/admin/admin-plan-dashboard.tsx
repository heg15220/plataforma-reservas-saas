"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import FormControlLabel from "@mui/material/FormControlLabel";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { type FormEvent, useEffect, useState } from "react";

import { Surface } from "@/components/layout";

import { type AdminPlan, type AdminPlanInput, fetchAdminPlans, saveAdminPlan } from "./admin-api";

type LimitKey = keyof AdminPlanInput["limits"];
const limitKeys: LimitKey[] = [
  "monthlyReservations",
  "teamResources",
  "customFormFields",
  "galleryImages",
];

/** Editor de planes que exige nombres y prestaciones equivalentes en ES/EN. */
export function AdminPlanDashboard() {
  const t = useTranslations("Admin.plans");
  const [plans, setPlans] = useState<AdminPlan[]>([]);
  const [selected, setSelected] = useState<AdminPlan | null>();
  const [error, setError] = useState(false);
  const [busy, setBusy] = useState(false);

  async function reload(signal?: AbortSignal) {
    try {
      setPlans((await fetchAdminPlans(signal)).plans);
      setError(false);
    } catch {
      setError(true);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const input: AdminPlanInput = {
      slug: String(data.get("slug")),
      nameEs: String(data.get("nameEs")),
      nameEn: String(data.get("nameEn")),
      priceMonthly: Number(data.get("priceMonthly")),
      priceYearly: Number(data.get("priceYearly")),
      limits: Object.fromEntries(
        limitKeys.map((key) => {
          const value = String(data.get(key) ?? "").trim();
          return [key, value === "" ? null : Number(value)];
        }),
      ) as AdminPlanInput["limits"],
      features: String(data.get("features"))
        .split("\n")
        .filter((line) => line.trim())
        .map((line) => {
          const [code, labelEs, labelEn] = line.split("|").map((part) => part.trim());
          return { code, labelEs, labelEn };
        }),
      active: data.get("active") === "on",
    };
    setBusy(true);
    try {
      await saveAdminPlan(input, selected?.id);
      setSelected(undefined);
      await reload();
    } catch {
      setError(true);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Stack spacing={3}>
      <div>
        <Typography component="h1" variant="h1">
          {t("title")}
        </Typography>
        <Typography color="text.secondary">{t("description")}</Typography>
      </div>
      {error && <Alert severity="error">{t("error")}</Alert>}
      <Button
        onClick={() => setSelected(null)}
        sx={{ alignSelf: "flex-start" }}
        variant="contained"
      >
        {t("create")}
      </Button>
      {selected !== undefined && (
        <Surface component="section">
          <Stack component="form" onSubmit={submit} spacing={2}>
            <Typography component="h2" variant="h2">
              {selected ? t("edit") : t("create")}
            </Typography>
            <TextField
              defaultValue={selected?.slug}
              disabled={Boolean(selected)}
              inputProps={{ pattern: "^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$" }}
              label={t("slug")}
              name="slug"
              required
            />
            <TextField defaultValue={selected?.nameEs} label={t("nameEs")} name="nameEs" required />
            <TextField defaultValue={selected?.nameEn} label={t("nameEn")} name="nameEn" required />
            <TextField
              defaultValue={selected?.priceMonthly}
              inputProps={{ min: 0, step: "0.01" }}
              label={t("priceMonthly")}
              name="priceMonthly"
              required
              type="number"
            />
            <TextField
              defaultValue={selected?.priceYearly}
              inputProps={{ min: 0, step: "0.01" }}
              label={t("priceYearly")}
              name="priceYearly"
              required
              type="number"
            />
            {limitKeys.map((key) => (
              <TextField
                defaultValue={selected?.limits[key] ?? ""}
                helperText={t("unlimited")}
                inputProps={{ min: 0, step: 1 }}
                key={key}
                label={t(`limits.${key}`)}
                name={key}
                type="number"
              />
            ))}
            <TextField
              defaultValue={selected?.features
                .map(({ code, labelEs, labelEn }) => `${code}|${labelEs}|${labelEn}`)
                .join("\n")}
              helperText={t("featuresHelp")}
              label={t("features")}
              multiline
              minRows={4}
              name="features"
              required
            />
            <FormControlLabel
              control={<Checkbox defaultChecked={selected?.active ?? true} name="active" />}
              label={t("active")}
            />
            <Stack direction="row" spacing={2}>
              <Button disabled={busy} type="submit" variant="contained">
                {t("save")}
              </Button>
              <Button onClick={() => setSelected(undefined)}>{t("cancel")}</Button>
            </Stack>
          </Stack>
        </Surface>
      )}
      {plans.map((plan) => (
        <Surface component="article" key={plan.id}>
          <Typography component="h2" variant="h2">
            {plan.nameEs} / {plan.nameEn}
          </Typography>
          <Typography>
            {plan.slug} · {plan.priceMonthly} / {plan.priceYearly}
          </Typography>
          <Typography color="text.secondary">
            {t(plan.active ? "enabled" : "disabled")} · {plan.features.length} {t("featuresCount")}
          </Typography>
          <Button onClick={() => setSelected(plan)} sx={{ mt: 2 }}>
            {t("edit")}
          </Button>
        </Surface>
      ))}
    </Stack>
  );
}
