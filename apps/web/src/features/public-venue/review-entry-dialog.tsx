"use client";

import Button from "@mui/material/Button";
import Dialog from "@mui/material/Dialog";
import DialogActions from "@mui/material/DialogActions";
import DialogContent from "@mui/material/DialogContent";
import DialogContentText from "@mui/material/DialogContentText";
import DialogTitle from "@mui/material/DialogTitle";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import { Star } from "lucide-react";
import { useTranslations } from "next-intl";
import { useState } from "react";

/**
 * Entrada pública al flujo de reseña.
 *
 * Esta iteración solicita el email sin ejecutar todavía la elegibilidad. El callback de
 * continuación se añadirá con el contrato por local/email de la tarea 11.10, evitando simular una
 * autorización únicamente en cliente o mostrar estrellas antes de acreditarla.
 */
export function ReviewEntryDialog() {
  const t = useTranslations("VenuePublicProfile.reviewForm");
  const [open, setOpen] = useState(false);

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
        maxWidth="xs"
        onClose={() => setOpen(false)}
        open={open}
        aria-describedby="review-entry-description"
      >
        <DialogTitle>{t("dialogTitle")}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 0.5 }}>
            <DialogContentText id="review-entry-description">
              {t("dialogDescription")}
            </DialogContentText>
            <TextField
              autoComplete="email"
              fullWidth
              label={t("emailLabel")}
              name="review-email"
              type="email"
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button onClick={() => setOpen(false)}>{t("cancel")}</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
