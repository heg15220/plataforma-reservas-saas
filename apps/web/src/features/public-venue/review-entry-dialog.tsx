"use client";

import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Checkbox from "@mui/material/Checkbox";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import FormControlLabel from "@mui/material/FormControlLabel";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import { Star } from "lucide-react";
import { useLocale, useTranslations } from "next-intl";
import { type FormEvent, useEffect, useRef, useState } from "react";

import {
  checkPublicReviewEligibility,
  createPublicVenueReview,
  PublicReviewApiError,
  type CreatedPublicReview,
} from "./public-review-api";
import { StarRatingInput } from "./star-rating-input";

/**
 * Entrada pública al flujo de reseña.
 *
 * Solicita email, acredita elegibilidad en backend y solo entonces muestra la captura de reseña.
 * La creación repite la selección transaccional; una comprobación previa nunca actúa como permiso.
 */
export function ReviewEntryDialog({ venueSlug }: { venueSlug: string }) {
  const t = useTranslations("VenuePublicProfile.reviewForm");
  const locale = useLocale();
  const [open, setOpen] = useState(false);
  const [step, setStep] = useState<"email" | "review" | "success">("email");
  const [email, setEmail] = useState("");
  const [rating, setRating] = useState<number | null>(null);
  const [comment, setComment] = useState("");
  const [accepted, setAccepted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [ratingError, setRatingError] = useState<string | undefined>();
  const [created, setCreated] = useState<CreatedPublicReview | null>(null);
  const controllerRef = useRef<AbortController | null>(null);

  useEffect(() => () => controllerRef.current?.abort(), []);

  const apiErrorMessage = (error: unknown) => {
    if (!(error instanceof PublicReviewApiError)) return t("errors.unavailable");
    if (error.kind === "alreadySubmitted") return t("errors.alreadySubmitted");
    if (error.kind === "notEligible") return t("errors.notEligible");
    if (error.kind === "invalid") return t("errors.invalid");
    return t("errors.unavailable");
  };

  const resetAndClose = () => {
    controllerRef.current?.abort();
    controllerRef.current = null;
    setOpen(false);
    setStep("email");
    setEmail("");
    setRating(null);
    setComment("");
    setAccepted(false);
    setBusy(false);
    setFeedback(null);
    setRatingError(undefined);
    setCreated(null);
  };

  const checkEligibility = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setBusy(true);
    setFeedback(null);
    const controller = new AbortController();
    controllerRef.current = controller;
    try {
      const result = await checkPublicReviewEligibility(
        venueSlug,
        email.trim(),
        controller.signal,
      );
      if (result.eligible) {
        setEmail(email.trim());
        setStep("review");
      } else {
        setFeedback(
          result.error === "REVIEW_ALREADY_SUBMITTED"
            ? t("errors.alreadySubmitted")
            : t("errors.notEligible"),
        );
      }
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      setFeedback(apiErrorMessage(error));
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
      setBusy(false);
    }
  };

  const publishReview = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setFeedback(null);
    if (rating === null) {
      setRatingError(t("errors.ratingRequired"));
      return;
    }
    setRatingError(undefined);
    if (!accepted) {
      setFeedback(t("errors.policyRequired"));
      return;
    }
    setBusy(true);
    const controller = new AbortController();
    controllerRef.current = controller;
    try {
      const result = await createPublicVenueReview(
        venueSlug,
        {
          acceptsReviewPolicy: true,
          comment: comment.trim() || null,
          customerEmail: email,
          rating,
        },
        controller.signal,
      );
      setCreated(result);
      setStep("success");
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") return;
      if (
        error instanceof PublicReviewApiError &&
        (error.kind === "notEligible" || error.kind === "alreadySubmitted")
      ) {
        setStep("email");
      }
      setFeedback(apiErrorMessage(error));
    } finally {
      if (controllerRef.current === controller) controllerRef.current = null;
      setBusy(false);
    }
  };

  return (
    <>
      <Button
        onClick={() => setOpen(true)}
        startIcon={<Star aria-hidden="true" />}
        variant="outlined"
      >
        {t("action")}
      </Button>
      <Dialog
        fullWidth
        maxWidth="sm"
        onClose={() => {
          if (!busy) resetAndClose();
        }}
        open={open}
        aria-describedby="review-entry-description"
      >
        <DialogTitle>
          {step === "success" ? t("successTitle") : t("dialogTitle")}
        </DialogTitle>
        {step === "email" && (
          <Box component="form" onSubmit={checkEligibility}>
            <DialogContent>
              <Stack spacing={2} sx={{ pt: 0.5 }}>
                <DialogContentText id="review-entry-description">
                  {t("dialogDescription")}
                </DialogContentText>
                {feedback && <Alert severity="info">{feedback}</Alert>}
                <TextField
                  autoComplete="email"
                  disabled={busy}
                  fullWidth
                  label={t("emailLabel")}
                  name="review-email"
                  onChange={(event) => setEmail(event.target.value)}
                  required
                  type="email"
                  value={email}
                />
              </Stack>
            </DialogContent>
            <DialogActions sx={{ p: 3 }}>
              <Button disabled={busy} onClick={resetAndClose}>
                {t("cancel")}
              </Button>
              <Button disabled={busy} type="submit" variant="contained">
                {busy ? t("checking") : t("checkAction")}
              </Button>
            </DialogActions>
          </Box>
        )}
        {step === "review" && (
          <Box component="form" onSubmit={publishReview}>
            <DialogContent>
              <Stack spacing={2.5} sx={{ pt: 0.5 }}>
                <DialogContentText id="review-entry-description">
                  {t("eligibleDescription")}
                </DialogContentText>
                {feedback && <Alert severity="error">{feedback}</Alert>}
                <StarRatingInput
                  disabled={busy}
                  error={ratingError}
                  onChange={(value) => {
                    setRating(value);
                    setRatingError(undefined);
                  }}
                  value={rating}
                />
                <TextField
                  disabled={busy}
                  fullWidth
                  helperText={t("commentHelper")}
                  label={t("commentLabel")}
                  maxRows={6}
                  minRows={3}
                  multiline
                  onChange={(event) => setComment(event.target.value)}
                  slotProps={{ htmlInput: { maxLength: 2000 } }}
                  value={comment}
                />
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={accepted}
                      disabled={busy}
                      onChange={(event) => setAccepted(event.target.checked)}
                    />
                  }
                  label={t("policyLabel")}
                />
              </Stack>
            </DialogContent>
            <DialogActions sx={{ p: 3 }}>
              <Button disabled={busy} onClick={() => setStep("email")}>
                {t("back")}
              </Button>
              <Button disabled={busy} type="submit" variant="contained">
                {busy ? t("publishing") : t("publish")}
              </Button>
            </DialogActions>
          </Box>
        )}
        {step === "success" && created && (
          <>
            <DialogContent>
              <Alert severity="success" id="review-entry-description">
                {t("success", { rating: created.rating })}
              </Alert>
              <DialogContentText sx={{ mt: 2 }}>
                {t("successSummary", {
                  average: new Intl.NumberFormat(locale, {
                    maximumFractionDigits: 1,
                    minimumFractionDigits: 1,
                  }).format(created.averageRating),
                  count: created.reviewsCount,
                })}
              </DialogContentText>
            </DialogContent>
            <DialogActions sx={{ p: 3 }}>
              <Button onClick={resetAndClose} variant="contained">
                {t("close")}
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </>
  );
}
