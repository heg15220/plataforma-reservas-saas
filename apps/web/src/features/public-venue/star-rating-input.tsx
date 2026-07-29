"use client";

import Box from "@mui/material/Box";
import FormHelperText from "@mui/material/FormHelperText";
import IconButton from "@mui/material/IconButton";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import { Star } from "lucide-react";
import { useTranslations } from "next-intl";
import { type KeyboardEvent, useId } from "react";

export interface StarRatingInputProps {
  value: number | null;
  onChange: (value: number) => void;
  disabled?: boolean;
  error?: string;
  name?: string;
}

/**
 * Selector controlado de una a cinco estrellas.
 *
 * Expone cada opción como radio accesible, conserva una etiqueta persistente y solo propaga
 * puntuaciones completas dentro del dominio admitido por el backend.
 */
export function StarRatingInput({
  value,
  onChange,
  disabled = false,
  error,
  name = "review-rating",
}: StarRatingInputProps) {
  const t = useTranslations("VenuePublicProfile.reviewForm");
  const labelId = useId();

  const chooseFromKeyboard = (
    event: KeyboardEvent<HTMLButtonElement>,
    currentRating: number,
  ) => {
    const nextRating = keyboardRating(event.key, currentRating);
    if (nextRating === null) return;
    event.preventDefault();
    onChange(nextRating);
    event.currentTarget.parentElement
      ?.querySelector<HTMLButtonElement>(`[data-rating="${nextRating}"]`)
      ?.focus();
  };

  return (
    <Stack spacing={0.75}>
      <Typography component="span" id={labelId} sx={{ fontWeight: 600 }}>
        {t("ratingLabel")}
      </Typography>
      <Box
        aria-labelledby={labelId}
        role="radiogroup"
        sx={{ display: "flex", gap: 0.25 }}
      >
        {[1, 2, 3, 4, 5].map((rating) => (
          <IconButton
            aria-checked={value === rating}
            aria-label={t("ratingOption", { rating })}
            data-rating={rating}
            disabled={disabled}
            key={rating}
            onClick={() => onChange(rating)}
            onKeyDown={(event) => chooseFromKeyboard(event, rating)}
            role="radio"
            tabIndex={value === rating || (value === null && rating === 1) ? 0 : -1}
            sx={{ color: rating <= (value ?? 0) ? "warning.main" : "action.disabled" }}
          >
            <Star
              aria-hidden="true"
              fill={rating <= (value ?? 0) ? "currentColor" : "none"}
            />
          </IconButton>
        ))}
      </Box>
      <input name={name} type="hidden" value={value ?? ""} />
      <FormHelperText error={Boolean(error)}>
        {error ?? t("ratingHelper")}
      </FormHelperText>
    </Stack>
  );
}

function keyboardRating(key: string, currentRating: number) {
  if (key === "Home") return 1;
  if (key === "End") return 5;
  if (key === "ArrowRight" || key === "ArrowUp") {
    return currentRating === 5 ? 1 : currentRating + 1;
  }
  if (key === "ArrowLeft" || key === "ArrowDown") {
    return currentRating === 1 ? 5 : currentRating - 1;
  }
  return null;
}
