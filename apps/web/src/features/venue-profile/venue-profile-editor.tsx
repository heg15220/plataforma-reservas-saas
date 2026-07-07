"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import CircularProgress from "@mui/material/CircularProgress";
import FormControlLabel from "@mui/material/FormControlLabel";
import MenuItem from "@mui/material/MenuItem";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import { ImagePlus, Send, Trash2 } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { type FormEvent, useCallback, useEffect, useRef, useState } from "react";

import { NavigationLink } from "@/components/navigation-link";
import { Surface } from "@/components/layout";

import {
  deleteGalleryImage,
  fetchVenueCategories,
  fetchVenueGallery,
  fetchVenueProfile,
  publishVenueProfile,
  resolveVenueAssetUrl,
  saveVenueProfile,
  uploadGalleryImage,
  uploadMainImage,
  VenueProfileApiError,
  type VenueCategory,
  type VenueGalleryImage,
  type VenueProfile,
  type VenueProfileApiErrorKind,
} from "./venue-profile-api";
import { parseVenueProfileForm, type VenueProfileFieldErrors } from "./venue-profile-schema";

type LoadState = "loading" | "ready" | "error";
type SubmitState = "idle" | "saving" | "publishing" | "uploadingMain" | "uploadingGallery";

/**
 * Panel privado de edición del perfil público del local.
 *
 * Carga datos desde el navegador para que la cookie HttpOnly viaje solo al API.
 * El formulario nunca acepta IDs de propietario ni estado arbitrario: creación,
 * edición, imágenes y publicación usan endpoints separados y autorizados.
 */
export function VenueProfileEditor() {
  const t = useTranslations("VenueProfileEditor");
  const locale = useLocale();
  const formRef = useRef<HTMLFormElement>(null);
  const galleryFileRef = useRef<HTMLInputElement>(null);
  const mainImageFileRef = useRef<HTMLInputElement>(null);
  const [loadState, setLoadState] = useState<LoadState>("loading");
  const [submitState, setSubmitState] = useState<SubmitState>("idle");
  const [profile, setProfile] = useState<VenueProfile | null>(null);
  const [categories, setCategories] = useState<VenueCategory[]>([]);
  const [gallery, setGallery] = useState<VenueGalleryImage[]>([]);
  const [selectedCategoryId, setSelectedCategoryId] = useState("");
  const [selectedDefaultLocale, setSelectedDefaultLocale] = useState("es");
  const [fieldErrors, setFieldErrors] = useState<VenueProfileFieldErrors>({});
  const [apiError, setApiError] = useState<VenueProfileApiError | null>(null);
  const [saved, setSaved] = useState(false);

  const load = useCallback(
    async (signal?: AbortSignal) => {
      setLoadState("loading");
      setApiError(null);
      try {
        const [nextCategories, nextProfile] = await Promise.all([
          fetchVenueCategories(locale, signal),
          fetchVenueProfile(signal),
        ]);
        const nextGallery = nextProfile ? await fetchVenueGallery(signal) : [];
        setCategories(nextCategories);
        setProfile(nextProfile);
        setGallery(nextGallery);
        setSelectedCategoryId(nextProfile?.categoryId ?? nextCategories[0]?.id ?? "");
        setSelectedDefaultLocale(nextProfile?.defaultLocale ?? (locale === "en" ? "en" : "es"));
        setLoadState("ready");
      } catch (error) {
        if (signal?.aborted) {
          return;
        }
        setApiError(toApiError(error));
        setLoadState("error");
      }
    },
    [locale],
  );

  useEffect(() => {
    const abortController = new AbortController();
    queueMicrotask(() => {
      if (!abortController.signal.aborted) {
        void load(abortController.signal);
      }
    });
    return () => abortController.abort();
  }, [load]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (submitState !== "idle") {
      return;
    }
    const result = parseVenueProfileForm(new FormData(event.currentTarget));
    if (!result.success) {
      setFieldErrors(result.errors);
      setSaved(false);
      setApiError(null);
      focusFirstInvalidField(result.errors);
      return;
    }

    setSubmitState("saving");
    setFieldErrors({});
    setApiError(null);
    setSaved(false);
    try {
      const nextProfile = await saveVenueProfile(result.payload, Boolean(profile));
      setProfile(nextProfile);
      setSaved(true);
    } catch (error) {
      setApiError(toApiError(error));
    } finally {
      setSubmitState("idle");
    }
  }

  async function handlePublish() {
    if (submitState !== "idle") {
      return;
    }
    setSubmitState("publishing");
    setApiError(null);
    setSaved(false);
    try {
      setProfile(await publishVenueProfile());
      setSaved(true);
    } catch (error) {
      setApiError(toApiError(error));
    } finally {
      setSubmitState("idle");
    }
  }

  async function handleMainImageUpload() {
    const file = mainImageFileRef.current?.files?.[0];
    if (!file || submitState !== "idle") {
      return;
    }
    setSubmitState("uploadingMain");
    setApiError(null);
    try {
      setProfile(await uploadMainImage(file));
      if (mainImageFileRef.current) {
        mainImageFileRef.current.value = "";
      }
    } catch (error) {
      setApiError(toApiError(error));
    } finally {
      setSubmitState("idle");
    }
  }

  async function handleGalleryUpload() {
    const file = galleryFileRef.current?.files?.[0];
    const altTextElement = formRef.current?.elements.namedItem("galleryAltText");
    const altText = altTextElement instanceof HTMLInputElement ? altTextElement.value.trim() : "";
    if (!file || !altText || submitState !== "idle") {
      setFieldErrors((current) => ({ ...current, galleryAltText: "required" }));
      return;
    }
    setSubmitState("uploadingGallery");
    setApiError(null);
    try {
      const image = await uploadGalleryImage(file, altText);
      setGallery((current) => [...current, image].sort((a, b) => a.position - b.position));
      if (galleryFileRef.current) {
        galleryFileRef.current.value = "";
      }
      if (altTextElement instanceof HTMLInputElement) {
        altTextElement.value = "";
      }
    } catch (error) {
      setApiError(toApiError(error));
    } finally {
      setSubmitState("idle");
    }
  }

  async function handleGalleryDelete(imageId: string) {
    if (submitState !== "idle") {
      return;
    }
    setApiError(null);
    try {
      await deleteGalleryImage(imageId);
      setGallery((current) => current.filter((image) => image.id !== imageId));
    } catch (error) {
      setApiError(toApiError(error));
    }
  }

  function focusFirstInvalidField(errors: VenueProfileFieldErrors) {
    const field = ["name", "categoryId", "defaultLocale", "descriptionI18n"].find(
      (candidate) => errors[candidate],
    );
    if (field) {
      const element = formRef.current?.elements.namedItem(field);
      if (element instanceof HTMLElement) {
        element.focus();
      }
    }
  }

  if (loadState === "loading") {
    return (
      <Surface>
        <Stack spacing={3} sx={{ alignItems: "center" }}>
          <CircularProgress aria-hidden="true" />
          <Typography>{t("loading")}</Typography>
        </Stack>
      </Surface>
    );
  }

  if (loadState === "error") {
    return (
      <Surface>
        <Stack spacing={4}>
          <Alert severity="error">
            {apiError ? apiErrorMessage(t, apiError.kind) : t("errors.api.unavailable")}
          </Alert>
          {apiError?.kind === "unauthenticated" ? (
            <Button component={NavigationLink} href="/locales/acceso" variant="contained">
              {t("actions.goToAccess")}
            </Button>
          ) : (
            <Button onClick={() => void load()} variant="outlined">
              {t("actions.retry")}
            </Button>
          )}
        </Stack>
      </Surface>
    );
  }

  const status = profile?.status ?? "draft";
  const mainImageUrl = resolveVenueAssetUrl(profile?.mainImageUrl ?? null);

  return (
    <Box component="form" noValidate onSubmit={handleSave} ref={formRef}>
      <Stack spacing={{ xs: 5, md: 6 }}>
        {apiError ? (
          <Alert aria-live="assertive" severity="error">
            {apiErrorMessage(t, apiError.kind)}
            {apiError.kind === "publicationRejected" && apiError.requirements.length > 0 ? (
              <Box component="ul" sx={{ mb: 0, mt: 2 }}>
                {apiError.requirements.map((requirement) => (
                  <li key={requirement}>{publicationRequirementMessage(t, requirement)}</li>
                ))}
              </Box>
            ) : null}
          </Alert>
        ) : null}
        {saved ? <Alert severity="success">{t("status.saved")}</Alert> : null}
        <input name="categoryId" type="hidden" value={selectedCategoryId} />
        <input name="defaultLocale" type="hidden" value={selectedDefaultLocale} />

        <Surface>
          <Stack spacing={4}>
            <SectionTitle title={t("sections.identity.title")} body={t("sections.identity.body")} />
            <TextField
              defaultValue={profile?.name ?? ""}
              error={Boolean(fieldErrors.name)}
              fullWidth
              helperText={
                fieldErrors.name ? fieldErrorMessage(t, fieldErrors.name) : t("fields.name.helper")
              }
              label={t("fields.name.label")}
              name="name"
              required
              slotProps={{ htmlInput: { maxLength: 160 } }}
            />
            <TextField
              defaultValue={selectedCategoryId}
              error={Boolean(fieldErrors.categoryId)}
              fullWidth
              helperText={
                fieldErrors.categoryId
                  ? fieldErrorMessage(t, fieldErrors.categoryId)
                  : t("fields.category.helper")
              }
              label={t("fields.category.label")}
              onChange={(event) => setSelectedCategoryId(event.target.value)}
              required
              select
              value={selectedCategoryId}
            >
              {categories.map((category) => (
                <MenuItem key={category.id} value={category.id}>
                  {category.name}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              fullWidth
              label={t("fields.defaultLocale.label")}
              onChange={(event) => setSelectedDefaultLocale(event.target.value)}
              select
              value={selectedDefaultLocale}
            >
              <MenuItem value="es">{t("locales.es")}</MenuItem>
              <MenuItem value="en">{t("locales.en")}</MenuItem>
            </TextField>
          </Stack>
        </Surface>

        <Surface>
          <Stack spacing={4}>
            <SectionTitle title={t("sections.texts.title")} body={t("sections.texts.body")} />
            <LocalizedTextFields
              errors={fieldErrors}
              label={t("fields.description.label")}
              name="description"
              profile={profile}
              rows={5}
            />
            <LocalizedTextFields
              errors={fieldErrors}
              label={t("fields.services.label")}
              name="services"
              profile={profile}
            />
            <LocalizedTextFields
              errors={fieldErrors}
              label={t("fields.rules.label")}
              name="rules"
              profile={profile}
            />
            <LocalizedTextFields
              errors={fieldErrors}
              label={t("fields.publicText.label")}
              name="publicText"
              profile={profile}
            />
          </Stack>
        </Surface>

        <Surface>
          <Stack spacing={4}>
            <SectionTitle title={t("sections.location.title")} body={t("sections.location.body")} />
            <TextField
              defaultValue={profile?.address ?? ""}
              fullWidth
              label={t("fields.address.label")}
              name="address"
            />
            <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
              <TextField
                defaultValue={profile?.city ?? ""}
                fullWidth
                label={t("fields.city.label")}
                name="city"
              />
              <TextField
                defaultValue={profile?.province ?? ""}
                fullWidth
                label={t("fields.province.label")}
                name="province"
              />
            </Stack>
            <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
              <TextField
                defaultValue={profile?.country ?? ""}
                fullWidth
                label={t("fields.country.label")}
                name="country"
              />
              <TextField
                defaultValue={profile?.postalCode ?? ""}
                fullWidth
                label={t("fields.postalCode.label")}
                name="postalCode"
              />
            </Stack>
            <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
              <TextField
                defaultValue={profile?.latitude ?? ""}
                fullWidth
                label={t("fields.latitude.label")}
                name="latitude"
              />
              <TextField
                defaultValue={profile?.longitude ?? ""}
                fullWidth
                label={t("fields.longitude.label")}
                name="longitude"
              />
            </Stack>
          </Stack>
        </Surface>

        <Surface>
          <Stack spacing={4}>
            <SectionTitle title={t("sections.contact.title")} body={t("sections.contact.body")} />
            <TextField
              defaultValue={profile?.contactEmail ?? ""}
              fullWidth
              label={t("fields.contactEmail.label")}
              name="contactEmail"
              type="email"
            />
            <TextField
              defaultValue={profile?.phone ?? ""}
              fullWidth
              label={t("fields.phone.label")}
              name="phone"
            />
            <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
              <FormControlLabel
                control={<Checkbox defaultChecked={profile?.showEmail ?? false} name="showEmail" />}
                label={t("fields.showEmail.label")}
              />
              <FormControlLabel
                control={<Checkbox defaultChecked={profile?.showPhone ?? false} name="showPhone" />}
                label={t("fields.showPhone.label")}
              />
            </Stack>
          </Stack>
        </Surface>

        <Surface>
          <Stack spacing={4}>
            <SectionTitle title={t("sections.images.title")} body={t("sections.images.body")} />
            {mainImageUrl ? (
              <Box
                alt={t("mainImageAlt")}
                component="img"
                src={mainImageUrl}
                sx={{ aspectRatio: "16 / 9", borderRadius: 3, objectFit: "cover", width: "100%" }}
              />
            ) : (
              <Alert severity="info">{t("images.noMainImage")}</Alert>
            )}
            <Stack direction={{ xs: "column", md: "row" }} spacing={2}>
              <Button component="label" startIcon={<ImagePlus size={18} />} variant="outlined">
                {t("actions.chooseMainImage")}
                <input accept="image/jpeg,image/png" hidden ref={mainImageFileRef} type="file" />
              </Button>
              <Button
                disabled={submitState !== "idle" || !profile}
                onClick={() => void handleMainImageUpload()}
                variant="contained"
              >
                {submitState === "uploadingMain"
                  ? t("actions.uploading")
                  : t("actions.uploadMainImage")}
              </Button>
            </Stack>

            <Box>
              <Typography component="h3" variant="h3">
                {t("sections.gallery.title")}
              </Typography>
              <Stack direction={{ xs: "column", md: "row" }} spacing={2} sx={{ mt: 3 }}>
                <TextField
                  error={Boolean(fieldErrors.galleryAltText)}
                  fullWidth
                  helperText={
                    fieldErrors.galleryAltText
                      ? t("errors.fields.required")
                      : t("fields.galleryAltText.helper")
                  }
                  label={t("fields.galleryAltText.label")}
                  name="galleryAltText"
                />
                <Button component="label" variant="outlined">
                  {t("actions.chooseGalleryImage")}
                  <input accept="image/jpeg,image/png" hidden ref={galleryFileRef} type="file" />
                </Button>
                <Button
                  disabled={submitState !== "idle" || !profile}
                  onClick={() => void handleGalleryUpload()}
                  variant="contained"
                >
                  {submitState === "uploadingGallery"
                    ? t("actions.uploading")
                    : t("actions.uploadGalleryImage")}
                </Button>
              </Stack>
            </Box>
            {gallery.length > 0 ? (
              <Box
                sx={{
                  display: "grid",
                  gap: 3,
                  gridTemplateColumns: { xs: "1fr", sm: "repeat(2, 1fr)" },
                }}
              >
                {gallery.map((image) => (
                  <Surface component="article" key={image.id} padded={false}>
                    <Box
                      alt={image.altText}
                      component="img"
                      src={resolveVenueAssetUrl(image.url) ?? image.url}
                      sx={{
                        aspectRatio: "4 / 3",
                        borderRadius: "inherit",
                        objectFit: "cover",
                        width: "100%",
                      }}
                    />
                    <Stack spacing={2} sx={{ p: 3 }}>
                      <Typography>{image.altText}</Typography>
                      <Button
                        color="error"
                        onClick={() => void handleGalleryDelete(image.id)}
                        startIcon={<Trash2 size={16} />}
                        variant="outlined"
                      >
                        {t("actions.deleteGalleryImage")}
                      </Button>
                    </Stack>
                  </Surface>
                ))}
              </Box>
            ) : (
              <Alert severity="info">{t("images.noGallery")}</Alert>
            )}
          </Stack>
        </Surface>

        <Surface>
          <Stack
            direction={{ xs: "column", md: "row" }}
            spacing={3}
            sx={{ alignItems: { md: "center" }, justifyContent: "space-between" }}
          >
            <Box>
              <Typography component="h2" variant="h2">
                {t("publication.title")}
              </Typography>
              <Typography color="text.secondary" sx={{ mt: 1 }}>
                {t("publication.status", { status })}
              </Typography>
            </Box>
            <Stack direction={{ xs: "column", sm: "row" }} spacing={2}>
              <Button
                disabled={submitState !== "idle"}
                size="large"
                type="submit"
                variant="contained"
              >
                {submitState === "saving" ? t("actions.saving") : t("actions.save")}
              </Button>
              <Button
                disabled={submitState !== "idle" || !profile}
                onClick={() => void handlePublish()}
                size="large"
                startIcon={<Send size={18} />}
                variant="outlined"
              >
                {submitState === "publishing" ? t("actions.publishing") : t("actions.publish")}
              </Button>
            </Stack>
          </Stack>
        </Surface>
      </Stack>
    </Box>
  );
}

function LocalizedTextFields({
  errors,
  label,
  name,
  profile,
  rows = 3,
}: {
  errors: VenueProfileFieldErrors;
  label: string;
  name: "description" | "services" | "rules" | "publicText";
  profile: VenueProfile | null;
  rows?: number;
}) {
  const t = useTranslations("VenueProfileEditor");
  const value = profile?.[`${name}I18n`];

  return (
    <Box>
      <Typography component="h3" variant="h3" sx={{ mb: 2 }}>
        {label}
      </Typography>
      <Stack direction={{ xs: "column", md: "row" }} spacing={3}>
        <TextField
          defaultValue={value?.values.es ?? ""}
          error={Boolean(errors[`${name}I18n`])}
          fullWidth
          helperText={
            errors[`${name}I18n`]
              ? fieldErrorMessage(t, errors[`${name}I18n`] ?? "invalid")
              : t("fields.localized.es")
          }
          label={`${label} · ${t("locales.es")}`}
          multiline
          name={`${name}_es`}
          rows={rows}
        />
        <TextField
          defaultValue={value?.values.en ?? ""}
          fullWidth
          helperText={t("fields.localized.en")}
          label={`${label} · ${t("locales.en")}`}
          multiline
          name={`${name}_en`}
          rows={rows}
        />
      </Stack>
    </Box>
  );
}

function SectionTitle({ body, title }: { body: string; title: string }) {
  return (
    <Box>
      <Typography component="h2" variant="h2">
        {title}
      </Typography>
      <Typography color="text.secondary" sx={{ mt: 1 }}>
        {body}
      </Typography>
    </Box>
  );
}

function toApiError(error: unknown): VenueProfileApiError {
  return error instanceof VenueProfileApiError ? error : new VenueProfileApiError("unavailable");
}

function apiErrorMessage(
  t: ReturnType<typeof useTranslations<"VenueProfileEditor">>,
  kind: VenueProfileApiErrorKind,
) {
  switch (kind) {
    case "conflict":
      return t("errors.api.conflict");
    case "descriptionTooLong":
      return t("errors.api.descriptionTooLong");
    case "forbidden":
      return t("errors.api.forbidden");
    case "galleryLimit":
      return t("errors.api.galleryLimit");
    case "imageInvalid":
      return t("errors.api.imageInvalid");
    case "invalid":
      return t("errors.api.invalid");
    case "notFound":
      return t("errors.api.notFound");
    case "publicationRejected":
      return t("errors.api.publicationRejected");
    case "rateLimited":
      return t("errors.api.rateLimited");
    case "unauthenticated":
      return t("errors.api.unauthenticated");
    case "unavailable":
      return t("errors.api.unavailable");
  }
}

function fieldErrorMessage(
  t: ReturnType<typeof useTranslations<"VenueProfileEditor">>,
  kind: NonNullable<VenueProfileFieldErrors[string]>,
) {
  switch (kind) {
    case "required":
      return t("errors.fields.required");
    case "tooLong":
      return t("errors.fields.tooLong");
    case "invalid":
      return t("errors.fields.invalid");
  }
}

function publicationRequirementMessage(
  t: ReturnType<typeof useTranslations<"VenueProfileEditor">>,
  requirement: string,
) {
  switch (requirement) {
    case "BUSINESS_VERIFICATION_NOT_APPROVED":
      return t("publicationRequirements.BUSINESS_VERIFICATION_NOT_APPROVED");
    case "CATEGORY_INACTIVE_OR_MISSING":
      return t("publicationRequirements.CATEGORY_INACTIVE_OR_MISSING");
    case "COORDINATES_MISSING":
      return t("publicationRequirements.COORDINATES_MISSING");
    case "EMAIL_NOT_VERIFIED":
      return t("publicationRequirements.EMAIL_NOT_VERIFIED");
    case "LOCALIZED_DESCRIPTION_MISSING":
      return t("publicationRequirements.LOCALIZED_DESCRIPTION_MISSING");
    case "LOCALIZED_PUBLIC_TEXT_INCOMPLETE":
      return t("publicationRequirements.LOCALIZED_PUBLIC_TEXT_INCOMPLETE");
    case "MAIN_IMAGE_MISSING":
      return t("publicationRequirements.MAIN_IMAGE_MISSING");
    case "MINIMUM_ADDRESS_MISSING":
      return t("publicationRequirements.MINIMUM_ADDRESS_MISSING");
    default:
      return requirement;
  }
}
