"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { Eye, EyeOff } from "lucide-react";
import { useTranslations } from "next-intl";
import { useRouter } from "next/navigation";
import { type FormEvent, useEffect, useRef, useState } from "react";

import { NavigationLink } from "@/components/navigation-link";

import { loginVenue, VenueLoginApiError, type VenueLoginErrorKind } from "./venue-login-api";
import { parseVenueLoginForm, type VenueLoginFieldErrors } from "./venue-login-schema";

type SubmissionState = "idle" | "submitting" | "redirecting";

const fieldErrorKeys = {
  email: "errors.fields.email",
  passwordBytes: "errors.fields.passwordBytes",
  required: "errors.fields.required",
} as const;

const submissionErrorKeys = {
  invalid: "errors.submission.invalid",
  rateLimited: "errors.submission.rateLimited",
  unavailable: "errors.submission.unavailable",
} as const;

/**
 * Formulario de acceso de propietarios conectado a la sesión opaca de backend.
 *
 * Mantiene las credenciales únicamente durante la interacción, evita doble
 * envío y navega con el locale de cuenta una vez creada la cookie HttpOnly.
 */
export function VenueLoginForm() {
  const t = useTranslations("VenueLogin");
  const router = useRouter();
  const formRef = useRef<HTMLFormElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const [fieldErrors, setFieldErrors] = useState<VenueLoginFieldErrors>({});
  const [submissionError, setSubmissionError] = useState<VenueLoginErrorKind>();
  const [submissionState, setSubmissionState] = useState<SubmissionState>("idle");
  const [showPassword, setShowPassword] = useState(false);

  useEffect(
    () => () => {
      abortControllerRef.current?.abort();
    },
    [],
  );

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;

    if (submissionState !== "idle") {
      return;
    }

    const result = parseVenueLoginForm(new FormData(form));
    if (!result.success) {
      setFieldErrors(result.errors);
      setSubmissionError(undefined);
      focusFirstInvalidField(result.errors);
      return;
    }

    setFieldErrors({});
    setSubmissionError(undefined);
    setSubmissionState("submitting");
    const abortController = new AbortController();
    abortControllerRef.current = abortController;

    try {
      const loginResult = await loginVenue(result.payload, abortController.signal);
      form.reset();
      setSubmissionState("redirecting");
      router.replace(`/panel?locale=${encodeURIComponent(loginResult.preferredLocale)}`);
    } catch (error) {
      if (abortController.signal.aborted) {
        return;
      }

      setSubmissionError(error instanceof VenueLoginApiError ? error.kind : "unavailable");
      setSubmissionState("idle");
    } finally {
      abortControllerRef.current = null;
    }
  }

  function clearFieldError(field: keyof VenueLoginFieldErrors) {
    setFieldErrors((current) => {
      if (!current[field]) {
        return current;
      }
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  function focusFirstInvalidField(errors: VenueLoginFieldErrors) {
    const field = errors.email ? "email" : errors.password ? "password" : undefined;
    const element = field ? formRef.current?.elements.namedItem(field) : null;
    if (element instanceof HTMLElement) {
      element.focus();
    }
  }

  const submitting = submissionState !== "idle";

  return (
    <Stack
      component="form"
      noValidate
      onSubmit={handleSubmit}
      ref={formRef}
      spacing={{ xs: 4, sm: 5 }}
    >
      <Stack spacing={1}>
        <Typography component="h2" variant="h2">
          {t("form.title")}
        </Typography>
        <Typography color="text.secondary">{t("form.description")}</Typography>
      </Stack>

      {submissionError ? (
        <Alert aria-live="assertive" severity="error">
          {t(submissionErrorKeys[submissionError])}
        </Alert>
      ) : null}

      <Stack spacing={{ xs: 3, sm: 4 }}>
        <TextField
          autoComplete="email"
          autoFocus
          error={Boolean(fieldErrors.email)}
          fullWidth
          helperText={fieldErrors.email ? t(fieldErrorKeys[fieldErrors.email]) : undefined}
          label={t("fields.email.label")}
          name="email"
          onChange={() => clearFieldError("email")}
          required
          slotProps={{ htmlInput: { maxLength: 320 } }}
          sx={{ "& .MuiOutlinedInput-root": { minHeight: 48 } }}
          type="email"
        />
        <TextField
          autoComplete="current-password"
          error={Boolean(fieldErrors.password)}
          fullWidth
          helperText={fieldErrors.password ? t(fieldErrorKeys[fieldErrors.password]) : undefined}
          label={t("fields.password.label")}
          name="password"
          onChange={() => clearFieldError("password")}
          required
          slotProps={{
            input: {
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label={
                      showPassword ? t("actions.hidePassword") : t("actions.showPassword")
                    }
                    edge="end"
                    onClick={() => setShowPassword((current) => !current)}
                    sx={{ minHeight: 44, minWidth: 44 }}
                    type="button"
                  >
                    {showPassword ? (
                      <EyeOff aria-hidden="true" size={20} />
                    ) : (
                      <Eye aria-hidden="true" size={20} />
                    )}
                  </IconButton>
                </InputAdornment>
              ),
            },
            htmlInput: { maxLength: 72 },
          }}
          sx={{ "& .MuiOutlinedInput-root": { minHeight: 48 } }}
          type={showPassword ? "text" : "password"}
        />
        <Link
          component={NavigationLink}
          href="/locales/recuperar-contrasena"
          sx={{ alignSelf: "flex-start" }}
        >
          {t("actions.forgotPassword")}
        </Link>
      </Stack>

      <Button
        disabled={submitting}
        fullWidth
        size="large"
        sx={{ minHeight: 48 }}
        type="submit"
        variant="contained"
      >
        {submitting ? (
          <>
            <CircularProgress aria-hidden="true" color="inherit" size={18} sx={{ mr: 2 }} />
            {t(submissionState === "redirecting" ? "actions.redirecting" : "actions.submitting")}
          </>
        ) : (
          t("actions.submit")
        )}
      </Button>

      <Typography color="text.secondary" sx={{ textAlign: "center" }}>
        {t("registration.prompt")}{" "}
        <Link component={NavigationLink} href="/locales/registro">
          {t("registration.action")}
        </Link>
      </Typography>
    </Stack>
  );
}
