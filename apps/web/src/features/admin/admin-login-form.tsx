"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { type FormEvent, useState } from "react";

import { AdminApiError, loginAdmin } from "./admin-api";

/** Formulario segregado que nunca acepta una sesión de propietario como sesión admin. */
export function AdminLoginForm() {
  const t = useTranslations("Admin.login");
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<"invalid" | "unavailable">();

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const data = new FormData(event.currentTarget);
    const email = String(data.get("email") ?? "").trim();
    const password = String(data.get("password") ?? "");
    if (!email || !password) {
      setError("invalid");
      return;
    }
    setBusy(true);
    setError(undefined);
    try {
      await loginAdmin(email, password);
      router.replace("/admin/categorias");
    } catch (reason) {
      setError(
        reason instanceof AdminApiError && reason.kind === "invalid" ? "invalid" : "unavailable",
      );
      setBusy(false);
    }
  }

  return (
    <Stack component="form" onSubmit={submit} spacing={3}>
      <Typography component="h1" variant="h1">
        {t("title")}
      </Typography>
      <Typography color="text.secondary">{t("description")}</Typography>
      {error && <Alert severity="error">{t(`errors.${error}`)}</Alert>}
      <TextField
        autoComplete="email"
        label={t("email")}
        name="email"
        required
        slotProps={{ htmlInput: { maxLength: 320 } }}
        type="email"
      />
      <TextField
        autoComplete="current-password"
        label={t("password")}
        name="password"
        required
        slotProps={{ htmlInput: { maxLength: 72 } }}
        type="password"
      />
      <Button disabled={busy} size="large" type="submit" variant="contained">
        {t(busy ? "submitting" : "submit")}
      </Button>
    </Stack>
  );
}
