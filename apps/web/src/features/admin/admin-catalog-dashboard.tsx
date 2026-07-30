"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import FormControlLabel from "@mui/material/FormControlLabel";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { type FormEvent, useCallback, useEffect, useState } from "react";
import type { ReactNode } from "react";

import { Surface } from "@/components/layout";

import {
  type AdminCategory,
  AdminApiError,
  type AdminVenue,
  fetchAdminCategories,
  fetchAdminVenues,
  saveAdminCategory,
  saveAdminVenue,
} from "./admin-api";

/** CRUD inicial responsive de categorías y datos básicos de locales. */
export function AdminCatalogDashboard({ mode }: { mode: "categories" | "venues" }) {
  const t = useTranslations("Admin");
  const [categories, setCategories] = useState<AdminCategory[]>([]);
  const [venues, setVenues] = useState<AdminVenue[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<AdminCategory>();
  const [selectedVenue, setSelectedVenue] = useState<AdminVenue>();
  const [error, setError] = useState<"forbidden" | "unavailable">();
  const [busy, setBusy] = useState(false);

  const reload = useCallback(
    async (signal?: AbortSignal) => {
      try {
        setError(undefined);
        const categoryResult = await fetchAdminCategories(signal);
        setCategories(categoryResult.categories);
        if (mode === "venues") {
          setVenues((await fetchAdminVenues(signal)).venues);
        }
      } catch (reason) {
        setError(
          reason instanceof AdminApiError && reason.kind === "forbidden"
            ? "forbidden"
            : "unavailable",
        );
      }
    },
    [mode],
  );

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, [reload]);

  async function submitCategory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    const data = new FormData(event.currentTarget);
    try {
      await saveAdminCategory(
        {
          slug: String(data.get("slug") ?? ""),
          nameEs: String(data.get("nameEs") ?? ""),
          nameEn: String(data.get("nameEn") ?? ""),
          active: data.get("active") === "on",
        },
        selectedCategory?.id,
      );
      setSelectedCategory(undefined);
      event.currentTarget.reset();
      await reload();
    } catch {
      setError("unavailable");
    } finally {
      setBusy(false);
    }
  }

  async function submitVenue(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedVenue) return;
    setBusy(true);
    const data = new FormData(event.currentTarget);
    const optional = (name: string) => String(data.get(name) ?? "").trim() || null;
    try {
      await saveAdminVenue(selectedVenue.id, {
        name: String(data.get("name") ?? ""),
        categoryId: String(data.get("categoryId") ?? ""),
        contactEmail: optional("contactEmail"),
        phone: optional("phone"),
        address: optional("address"),
        city: optional("city"),
        province: optional("province"),
        country: optional("country")?.toUpperCase() ?? null,
        postalCode: optional("postalCode"),
      });
      setSelectedVenue(undefined);
      await reload();
    } catch {
      setError("unavailable");
    } finally {
      setBusy(false);
    }
  }

  if (error === "forbidden") return <Alert severity="error">{t("errors.forbidden")}</Alert>;

  return (
    <Stack spacing={4}>
      <Box>
        <Typography component="h1" variant="h1">
          {t(`${mode}.title`)}
        </Typography>
        <Typography color="text.secondary" sx={{ mt: 1 }}>
          {t(`${mode}.description`)}
        </Typography>
      </Box>
      {error && <Alert severity="error">{t("errors.unavailable")}</Alert>}
      {mode === "categories" ? (
        <>
          <Surface component="section">
            <Stack
              component="form"
              key={selectedCategory?.id ?? "new"}
              onSubmit={submitCategory}
              spacing={2}
            >
              <Typography component="h2" variant="h2">
                {t(selectedCategory ? "categories.edit" : "categories.create")}
              </Typography>
              <TextField
                defaultValue={selectedCategory?.slug}
                label={t("categories.slug")}
                name="slug"
                required
              />
              <TextField
                defaultValue={selectedCategory?.nameEs}
                label={t("categories.nameEs")}
                name="nameEs"
                required
              />
              <TextField
                defaultValue={selectedCategory?.nameEn}
                label={t("categories.nameEn")}
                name="nameEn"
                required
              />
              <FormControlLabel
                control={
                  <Checkbox defaultChecked={selectedCategory?.active ?? true} name="active" />
                }
                label={t("categories.active")}
              />
              <Stack direction="row" spacing={2}>
                <Button disabled={busy} type="submit" variant="contained">
                  {t("actions.save")}
                </Button>
                {selectedCategory && (
                  <Button onClick={() => setSelectedCategory(undefined)}>
                    {t("actions.cancel")}
                  </Button>
                )}
              </Stack>
            </Stack>
          </Surface>
          <ItemGrid>
            {categories.map((category) => (
              <Surface component="article" key={category.id}>
                <Typography component="h2" variant="h2">
                  {category.nameEs}
                </Typography>
                <Typography color="text.secondary">
                  {category.nameEn} · {category.slug}
                </Typography>
                <Typography sx={{ mt: 1 }}>
                  {t(category.active ? "categories.enabled" : "categories.disabled")}
                </Typography>
                <Button onClick={() => setSelectedCategory(category)} sx={{ mt: 2 }}>
                  {t("actions.edit")}
                </Button>
              </Surface>
            ))}
          </ItemGrid>
        </>
      ) : (
        <>
          {selectedVenue && (
            <Surface component="section">
              <Stack component="form" key={selectedVenue.id} onSubmit={submitVenue} spacing={2}>
                <Typography component="h2" variant="h2">
                  {t("venues.edit")}
                </Typography>
                <TextField
                  defaultValue={selectedVenue.name}
                  label={t("venues.fields.name")}
                  name="name"
                  required
                />
                <TextField
                  defaultValue={selectedVenue.categoryId}
                  label={t("venues.fields.category")}
                  name="categoryId"
                  required
                  select
                >
                  {categories
                    .filter((category) => category.active)
                    .map((category) => (
                      <MenuItem key={category.id} value={category.id}>
                        {category.nameEs}
                      </MenuItem>
                    ))}
                </TextField>
                {(
                  [
                    "contactEmail",
                    "phone",
                    "address",
                    "city",
                    "province",
                    "country",
                    "postalCode",
                  ] as const
                ).map((field) => (
                  <TextField
                    defaultValue={selectedVenue[field] ?? ""}
                    key={field}
                    label={t(`venues.fields.${field}`)}
                    name={field}
                  />
                ))}
                <Stack direction="row" spacing={2}>
                  <Button disabled={busy} type="submit" variant="contained">
                    {t("actions.save")}
                  </Button>
                  <Button onClick={() => setSelectedVenue(undefined)}>{t("actions.cancel")}</Button>
                </Stack>
              </Stack>
            </Surface>
          )}
          <ItemGrid>
            {venues.map((venue) => (
              <Surface component="article" key={venue.id}>
                <Typography component="h2" variant="h2">
                  {venue.name}
                </Typography>
                <Typography color="text.secondary">
                  {venue.categoryName} · {venue.city ?? t("venues.noCity")}
                </Typography>
                <Typography sx={{ mt: 1 }}>
                  {t("venues.status", { status: venue.status })}
                </Typography>
                <Button onClick={() => setSelectedVenue(venue)} sx={{ mt: 2 }}>
                  {t("actions.edit")}
                </Button>
              </Surface>
            ))}
          </ItemGrid>
        </>
      )}
    </Stack>
  );
}

function ItemGrid({ children }: { children: ReactNode }) {
  return (
    <Box sx={{ display: "grid", gap: 3, gridTemplateColumns: { lg: "repeat(2, minmax(0, 1fr))" } }}>
      {children}
    </Box>
  );
}
