"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import CircularProgress from "@mui/material/CircularProgress";
import FormControl from "@mui/material/FormControl";
import FormControlLabel from "@mui/material/FormControlLabel";
import FormHelperText from "@mui/material/FormHelperText";
import FormLabel from "@mui/material/FormLabel";
import Link from "@mui/material/Link";
import Radio from "@mui/material/Radio";
import RadioGroup from "@mui/material/RadioGroup";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { CheckCircle2, FileCheck2, FileUp, LockKeyhole, RefreshCw, Trash2 } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { type ChangeEvent, type FormEvent, useEffect, useRef, useState } from "react";

import { NavigationLink } from "@/components/navigation-link";
import { Surface } from "@/components/layout";
import { StatusChip } from "@/components/visual";
import { visualTokens } from "@/theme/visual-tokens";

import {
  type BusinessDocumentApiErrorKind,
  type BusinessDocumentRequest,
  type BusinessDocumentType,
  BusinessDocumentApiError,
  fetchBusinessDocumentRequest,
  uploadBusinessDocument,
} from "./business-document-api";
import {
  type BusinessDocumentFileError,
  maximumBusinessDocumentBytes,
  validateBusinessDocumentFile,
} from "./business-document-file";

type LoadState =
  | { kind: "loading" }
  | { kind: "noRequest" }
  | { kind: "ready"; request: BusinessDocumentRequest }
  | { kind: "error"; error: BusinessDocumentApiErrorKind }
  | { kind: "uploaded"; uploadedAt: string };

const reasonKeys = {
  no_automated_channel: "reasons.noAutomatedChannel",
  provider_unavailable: "reasons.providerUnavailable",
  insufficient_provider_data: "reasons.insufficientProviderData",
  legal_name_unconfirmed: "reasons.legalNameUnconfirmed",
  address_unconfirmed: "reasons.addressUnconfirmed",
} as const;

const documentTypeKeys = {
  census_registration_036_037: "documentTypes.censusRegistration",
  census_certificate: "documentTypes.censusCertificate",
  activity_or_opening_license: "documentTypes.activityLicense",
  equivalent_administrative_document: "documentTypes.equivalentDocument",
  other: "documentTypes.other",
} as const;

const apiErrorKeys = {
  unauthenticated: "errors.api.unauthenticated",
  forbidden: "errors.api.forbidden",
  invalid: "errors.api.invalid",
  conflict: "errors.api.conflict",
  malware: "errors.api.malware",
  rateLimited: "errors.api.rateLimited",
  unavailable: "errors.api.unavailable",
} as const;

const fileErrorKeys = {
  empty: "errors.file.empty",
  tooLarge: "errors.file.tooLarge",
  unsupportedType: "errors.file.unsupportedType",
} as const;

/**
 * Estado cliente del portal documental autenticado.
 *
 * Carga la solicitud, valida una selección efímera y transmite un único fichero.
 * No conserva contenido, nombre ni respuesta en almacenamiento del navegador.
 */
export function BusinessDocumentUpload() {
  const t = useTranslations("BusinessDocuments");
  const locale = useLocale();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const loadAbortRef = useRef<AbortController | null>(null);
  const uploadAbortRef = useRef<AbortController | null>(null);
  const [loadState, setLoadState] = useState<LoadState>({ kind: "loading" });
  const [documentType, setDocumentType] = useState<BusinessDocumentType>();
  const [file, setFile] = useState<File>();
  const [fileError, setFileError] = useState<BusinessDocumentFileError>();
  const [uploadError, setUploadError] = useState<BusinessDocumentApiErrorKind>();
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    void loadRequest();
    return () => {
      loadAbortRef.current?.abort();
      uploadAbortRef.current?.abort();
    };
  }, []);

  async function loadRequest() {
    loadAbortRef.current?.abort();
    const controller = new AbortController();
    loadAbortRef.current = controller;
    setLoadState({ kind: "loading" });

    try {
      const request = await fetchBusinessDocumentRequest(controller.signal);
      if (request) {
        setDocumentType(request.requestedDocumentTypes[0]);
        setLoadState({ kind: "ready", request });
      } else {
        setLoadState({ kind: "noRequest" });
      }
    } catch (error) {
      if (!controller.signal.aborted) {
        setLoadState({ kind: "error", error: apiErrorKind(error) });
      }
    } finally {
      if (loadAbortRef.current === controller) {
        loadAbortRef.current = null;
      }
    }
  }

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const selected = event.target.files?.[0];
    setUploadError(undefined);
    if (!selected) {
      setFile(undefined);
      setFileError(undefined);
      return;
    }

    const validationError = validateBusinessDocumentFile(selected);
    if (validationError) {
      setFile(undefined);
      setFileError(validationError);
      event.target.value = "";
      return;
    }

    setFile(selected);
    setFileError(undefined);
  }

  function clearFile() {
    setFile(undefined);
    setFileError(undefined);
    setUploadError(undefined);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (loadState.kind !== "ready" || uploading) {
      return;
    }
    if (!file) {
      setFileError("empty");
      fileInputRef.current?.focus();
      return;
    }
    if (!documentType) {
      return;
    }

    const controller = new AbortController();
    uploadAbortRef.current = controller;
    setUploading(true);
    setUploadError(undefined);

    try {
      const result = await uploadBusinessDocument(
        loadState.request.requestId,
        documentType,
        file,
        controller.signal,
      );
      clearFile();
      setLoadState({ kind: "uploaded", uploadedAt: result.uploadedAt });
    } catch (error) {
      if (!controller.signal.aborted) {
        setUploadError(apiErrorKind(error));
      }
    } finally {
      if (uploadAbortRef.current === controller) {
        uploadAbortRef.current = null;
      }
      setUploading(false);
    }
  }

  if (loadState.kind === "loading") {
    return (
      <Surface>
        <Stack aria-live="polite" spacing={3} sx={{ alignItems: "center", py: 8 }}>
          <CircularProgress size={28} />
          <Typography color="text.secondary">{t("loading")}</Typography>
        </Stack>
      </Surface>
    );
  }

  if (loadState.kind === "error") {
    const unauthenticated = loadState.error === "unauthenticated";
    return (
      <Surface>
        <Stack spacing={4}>
          <Alert severity="error">{t(apiErrorKeys[loadState.error])}</Alert>
          {unauthenticated ? (
            <Button component={NavigationLink} href="/locales/acceso" variant="contained">
              {t("actions.goToAccess")}
            </Button>
          ) : (
            <Button
              onClick={() => void loadRequest()}
              startIcon={<RefreshCw aria-hidden="true" size={18} />}
              variant="outlined"
            >
              {t("actions.retry")}
            </Button>
          )}
        </Stack>
      </Surface>
    );
  }

  if (loadState.kind === "noRequest") {
    return (
      <Surface>
        <Stack spacing={4}>
          <StatusChip label={t("status.noRequest")} tone="success" />
          <Box>
            <Typography component="h2" gutterBottom variant="h2">
              {t("noRequest.title")}
            </Typography>
            <Typography color="text.secondary">{t("noRequest.body")}</Typography>
          </Box>
        </Stack>
      </Surface>
    );
  }

  if (loadState.kind === "uploaded") {
    return (
      <Surface>
        <Stack aria-live="polite" spacing={4}>
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
            <CheckCircle2 aria-hidden="true" size={28} />
          </Box>
          <Box>
            <Typography component="h2" gutterBottom variant="h2">
              {t("success.title")}
            </Typography>
            <Typography color="text.secondary">{t("success.body")}</Typography>
          </Box>
          <StatusChip label={t("status.pendingReview")} tone="warning" />
          <Typography color="text.secondary" variant="body2">
            {t("success.uploadedAt", {
              date: formatUtcDate(loadState.uploadedAt, locale),
            })}
          </Typography>
        </Stack>
      </Surface>
    );
  }

  const request = loadState.request;
  return (
    <Stack spacing={5}>
      <Alert severity="warning">
        <Typography component="h2" sx={{ fontWeight: 600 }} variant="body1">
          {t("request.title")}
        </Typography>
        <Typography variant="body2">{t(reasonKeys[request.reasonCode])}</Typography>
      </Alert>

      <Surface>
        <Box component="form" noValidate onSubmit={handleSubmit}>
          <Stack spacing={6}>
            <Box>
              <Stack
                direction={{ xs: "column", sm: "row" }}
                spacing={2}
                sx={{ alignItems: { sm: "center" }, justifyContent: "space-between" }}
              >
                <Typography component="h2" variant="h2">
                  {t("form.title")}
                </Typography>
                <StatusChip label={t("status.actionRequired")} tone="warning" />
              </Stack>
              <Typography color="text.secondary" sx={{ mt: 2 }}>
                {t("form.requestedAt", {
                  date: formatUtcDate(request.requestedAt, locale),
                })}
              </Typography>
            </Box>

            <FormControl required>
              <FormLabel>{t("form.documentTypeLabel")}</FormLabel>
              <RadioGroup
                name="documentType"
                onChange={(event) => {
                  setDocumentType(event.target.value as BusinessDocumentType);
                  setUploadError(undefined);
                }}
                value={documentType ?? ""}
              >
                {request.requestedDocumentTypes.map((type) => (
                  <FormControlLabel
                    control={<Radio />}
                    key={type}
                    label={t(documentTypeKeys[type])}
                    value={type}
                  />
                ))}
              </RadioGroup>
              <FormHelperText>{t("form.documentTypeHelper")}</FormHelperText>
            </FormControl>

            <Box>
              <FormLabel component="span">{t("form.fileLabel")}</FormLabel>
              <Box
                sx={{
                  bgcolor: "background.default",
                  border: 1,
                  borderColor: fileError ? "error.main" : "divider",
                  borderRadius: `${visualTokens.radius.card}px`,
                  mt: 2,
                  p: { xs: 4, sm: 5 },
                }}
              >
                <Stack spacing={3}>
                  <FileUp aria-hidden="true" color={visualTokens.color.brand.primary} size={28} />
                  <Box>
                    <Typography sx={{ fontWeight: 600 }}>{t("form.filePrompt")}</Typography>
                    <Typography color="text.secondary" variant="body2">
                      {t("form.fileConstraints", {
                        maxSize: formatFileSize(maximumBusinessDocumentBytes, locale),
                      })}
                    </Typography>
                  </Box>
                  <Button component="label" sx={{ alignSelf: "flex-start" }} variant="outlined">
                    {t("actions.chooseFile")}
                    <input
                      accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg"
                      hidden
                      onChange={handleFileChange}
                      ref={fileInputRef}
                      type="file"
                    />
                  </Button>
                </Stack>
              </Box>
              {fileError ? (
                <FormHelperText error>{t(fileErrorKeys[fileError])}</FormHelperText>
              ) : null}
            </Box>

            {file ? (
              <Stack
                aria-live="polite"
                direction="row"
                spacing={3}
                sx={{
                  alignItems: "center",
                  border: 1,
                  borderColor: "divider",
                  borderRadius: `${visualTokens.radius.control}px`,
                  p: 3,
                }}
              >
                <FileCheck2 aria-hidden="true" size={22} />
                <Box sx={{ minWidth: 0 }}>
                  <Typography noWrap sx={{ fontWeight: 600 }}>
                    {file.name}
                  </Typography>
                  <Typography color="text.secondary" variant="body2">
                    {formatFileSize(file.size, locale)}
                  </Typography>
                </Box>
                <Button
                  aria-label={t("actions.removeFile")}
                  color="inherit"
                  onClick={clearFile}
                  size="small"
                  sx={{ ml: "auto", minWidth: 44 }}
                  type="button"
                >
                  <Trash2 aria-hidden="true" size={18} />
                </Button>
              </Stack>
            ) : null}

            <Alert icon={<LockKeyhole aria-hidden="true" size={20} />} severity="info">
              {t("form.privacyNotice")}
            </Alert>

            {uploadError ? (
              <Alert aria-live="assertive" severity="error">
                {t(apiErrorKeys[uploadError])}
                {uploadError === "unauthenticated" ? (
                  <>
                    {" "}
                    <Link component={NavigationLink} href="/locales/acceso">
                      {t("actions.goToAccess")}
                    </Link>
                  </>
                ) : null}
              </Alert>
            ) : null}

            <Button disabled={uploading} fullWidth size="large" type="submit" variant="contained">
              {uploading ? (
                <>
                  <CircularProgress aria-hidden="true" color="inherit" size={18} sx={{ mr: 2 }} />
                  {t("actions.uploading")}
                </>
              ) : (
                t("actions.upload")
              )}
            </Button>
          </Stack>
        </Box>
      </Surface>
    </Stack>
  );
}

function apiErrorKind(error: unknown): BusinessDocumentApiErrorKind {
  return error instanceof BusinessDocumentApiError ? error.kind : "unavailable";
}

function formatUtcDate(value: string, locale: string) {
  return new Intl.DateTimeFormat(locale, {
    dateStyle: "medium",
    timeZone: "UTC",
  }).format(new Date(value));
}

function formatFileSize(bytes: number, locale: string) {
  const megabytes = bytes / (1024 * 1024);
  return new Intl.NumberFormat(locale, {
    maximumFractionDigits: megabytes >= 1 ? 1 : 2,
    style: "unit",
    unit: "megabyte",
    unitDisplay: "short",
  }).format(megabytes);
}
