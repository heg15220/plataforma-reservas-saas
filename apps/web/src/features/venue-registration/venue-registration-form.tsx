"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import CircularProgress from "@mui/material/CircularProgress";
import FormControlLabel from "@mui/material/FormControlLabel";
import FormHelperText from "@mui/material/FormHelperText";
import IconButton from "@mui/material/IconButton";
import InputAdornment from "@mui/material/InputAdornment";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { Eye, EyeOff, ShieldCheck } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { type FormEvent, useEffect, useRef, useState } from "react";

import { NavigationLink } from "@/components/navigation-link";
import { VerificationStatusSummary } from "@/features/verification/verification-status-summary";
import type { SupportedLocale } from "@/i18n/config";

import {
  registerVenue,
  VenueRegistrationApiError,
  type VenueRegistrationResult,
} from "./venue-registration-api";
import {
  parseVenueRegistrationForm,
  type RegistrationFieldErrors,
} from "./venue-registration-schema";

type SubmissionState = "idle" | "submitting" | "success";

const errorFields = [
  "email",
  "password",
  "taxCountry",
  "legalName",
  "taxIdentifier",
  "registeredAddress",
  "acceptsLegalTerms",
] as const;

const fieldErrorKeys = {
  country: "errors.fields.country",
  email: "errors.fields.email",
  legalTerms: "errors.fields.legalTerms",
  passwordBytes: "errors.fields.passwordBytes",
  passwordLength: "errors.fields.passwordLength",
  required: "errors.fields.required",
  tooLong: "errors.fields.tooLong",
} as const;

const submissionErrorKeys = {
  conflict: "errors.submission.conflict",
  invalid: "errors.submission.invalid",
  rateLimited: "errors.submission.rateLimited",
  unavailable: "errors.submission.unavailable",
} as const;

/**
 * Formulario público de alta de una cuenta de local.
 *
 * Gestiona validación contextual y el ciclo de la petición, pero delega en la
 * API la normalización fiscal, unicidad y verificación empresarial definitivas.
 */
export function VenueRegistrationForm() {
  const t = useTranslations("VenueRegistration");
  const locale = useLocale() as SupportedLocale;
  const formRef = useRef<HTMLFormElement>(null);
  const abortControllerRef = useRef<AbortController | null>(null);
  const [fieldErrors, setFieldErrors] = useState<RegistrationFieldErrors>({});
  const [submissionState, setSubmissionState] = useState<SubmissionState>("idle");
  const [submissionError, setSubmissionError] = useState<keyof typeof submissionErrorKeys>();
  const [registrationResult, setRegistrationResult] = useState<VenueRegistrationResult>();
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

    if (submissionState === "submitting") {
      return;
    }

    const result = parseVenueRegistrationForm(new FormData(form), locale);

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
      const response = await registerVenue(result.payload, abortController.signal);
      form.reset();
      setRegistrationResult(response);
      setSubmissionState("success");
    } catch (error) {
      if (abortController.signal.aborted) {
        return;
      }

      const kind = error instanceof VenueRegistrationApiError ? error.kind : "unavailable";
      setSubmissionError(kind);
      setSubmissionState("idle");
    } finally {
      abortControllerRef.current = null;
    }
  }

  function clearFieldError(field: keyof RegistrationFieldErrors) {
    setFieldErrors((current) => {
      if (!current[field]) {
        return current;
      }
      const next = { ...current };
      delete next[field];
      return next;
    });
  }

  function focusFirstInvalidField(errors: RegistrationFieldErrors) {
    const field = errorFields.find((candidate) => errors[candidate]);
    if (field) {
      const element = formRef.current?.elements.namedItem(field);
      if (element instanceof HTMLElement) {
        element.focus();
      }
    }
  }

  if (submissionState === "success" && registrationResult) {
    return (
      <Stack aria-live="polite" spacing={4} sx={{ py: { xs: 2, md: 4 } }}>
        <Box
          sx={{
            alignItems: "center",
            bgcolor: "success.light",
            borderRadius: 999,
            color: "success.dark",
            display: "flex",
            height: 56,
            justifyContent: "center",
            width: 56,
          }}
        >
          <ShieldCheck aria-hidden="true" size={28} />
        </Box>
        <Box>
          <Typography component="h2" gutterBottom variant="h2">
            {t("success.title")}
          </Typography>
          <Typography color="text.secondary">{t("success.body")}</Typography>
        </Box>
        <Alert severity="info">{t("success.verificationNotice")}</Alert>
        <VerificationStatusSummary
          businessStatus={registrationResult.businessVerificationStatus}
          emailVerified={!registrationResult.emailVerificationRequired}
        />
        <Button component={NavigationLink} href="/locales/acceso" variant="outlined">
          {t("actions.goToAccess")}
        </Button>
      </Stack>
    );
  }

  return (
    <Box component="form" noValidate onSubmit={handleSubmit} ref={formRef}>
      <Stack spacing={6}>
        {submissionError ? (
          <Alert aria-live="assertive" severity="error">
            {t(submissionErrorKeys[submissionError])}
          </Alert>
        ) : null}

        <Box component="fieldset" sx={{ border: 0, m: 0, p: 0 }}>
          <Typography component="legend" gutterBottom variant="h2">
            {t("sections.account.title")}
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 4 }}>
            {t("sections.account.description")}
          </Typography>
          <Stack spacing={4}>
            <TextField
              autoComplete="email"
              error={Boolean(fieldErrors.email)}
              fullWidth
              helperText={
                fieldErrors.email ? t(fieldErrorKeys[fieldErrors.email]) : t("fields.email.helper")
              }
              label={t("fields.email.label")}
              name="email"
              onChange={() => clearFieldError("email")}
              required
              type="email"
            />
            <TextField
              autoComplete="new-password"
              error={Boolean(fieldErrors.password)}
              fullWidth
              helperText={
                fieldErrors.password
                  ? t(fieldErrorKeys[fieldErrors.password])
                  : t("fields.password.helper")
              }
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
                htmlInput: { maxLength: 72, minLength: 12 },
              }}
              type={showPassword ? "text" : "password"}
            />
          </Stack>
        </Box>

        <Box component="fieldset" sx={{ border: 0, m: 0, p: 0 }}>
          <Typography component="legend" gutterBottom variant="h2">
            {t("sections.business.title")}
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 4 }}>
            {t("sections.business.description")}
          </Typography>
          <Stack spacing={4}>
            <TextField
              autoCapitalize="characters"
              autoComplete="country"
              defaultValue="ES"
              error={Boolean(fieldErrors.taxCountry)}
              fullWidth
              helperText={
                fieldErrors.taxCountry
                  ? t(fieldErrorKeys[fieldErrors.taxCountry])
                  : t("fields.taxCountry.helper")
              }
              label={t("fields.taxCountry.label")}
              name="taxCountry"
              onChange={() => clearFieldError("taxCountry")}
              required
              slotProps={{ htmlInput: { maxLength: 2 } }}
            />
            <TextField
              autoComplete="organization"
              error={Boolean(fieldErrors.legalName)}
              fullWidth
              helperText={
                fieldErrors.legalName
                  ? t(fieldErrorKeys[fieldErrors.legalName])
                  : t("fields.legalName.helper")
              }
              label={t("fields.legalName.label")}
              name="legalName"
              onChange={() => clearFieldError("legalName")}
              required
              slotProps={{ htmlInput: { maxLength: 255 } }}
            />
            <TextField
              error={Boolean(fieldErrors.taxIdentifier)}
              fullWidth
              helperText={
                fieldErrors.taxIdentifier
                  ? t(fieldErrorKeys[fieldErrors.taxIdentifier])
                  : t("fields.taxIdentifier.helper")
              }
              label={t("fields.taxIdentifier.label")}
              name="taxIdentifier"
              onChange={() => clearFieldError("taxIdentifier")}
              required
              slotProps={{ htmlInput: { maxLength: 64 } }}
            />
            <TextField
              autoComplete="street-address"
              error={Boolean(fieldErrors.registeredAddress)}
              fullWidth
              helperText={
                fieldErrors.registeredAddress
                  ? t(fieldErrorKeys[fieldErrors.registeredAddress])
                  : t("fields.registeredAddress.helper")
              }
              label={t("fields.registeredAddress.label")}
              minRows={2}
              multiline
              name="registeredAddress"
              onChange={() => clearFieldError("registeredAddress")}
              slotProps={{ htmlInput: { maxLength: 500 } }}
            />
          </Stack>
        </Box>

        <Box>
          <FormControlLabel
            control={
              <Checkbox
                name="acceptsLegalTerms"
                onChange={() => clearFieldError("acceptsLegalTerms")}
                required
              />
            }
            label={
              <Typography variant="body1">
                {t.rich("fields.legalTerms.label", {
                  terms: (chunks) => (
                    <Link component={NavigationLink} href="/legal/condiciones">
                      {chunks}
                    </Link>
                  ),
                  privacy: (chunks) => (
                    <Link component={NavigationLink} href="/legal/privacidad">
                      {chunks}
                    </Link>
                  ),
                })}
              </Typography>
            }
          />
          {fieldErrors.acceptsLegalTerms ? (
            <FormHelperText error>{t("errors.fields.legalTerms")}</FormHelperText>
          ) : null}
        </Box>

        <Button
          disabled={submissionState === "submitting"}
          fullWidth
          size="large"
          type="submit"
          variant="contained"
        >
          {submissionState === "submitting" ? (
            <>
              <CircularProgress aria-hidden="true" color="inherit" size={18} sx={{ mr: 2 }} />
              {t("actions.submitting")}
            </>
          ) : (
            t("actions.submit")
          )}
        </Button>
      </Stack>
    </Box>
  );
}
