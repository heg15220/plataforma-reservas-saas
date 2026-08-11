"use client";

import Alert from "@mui/material/Alert";
import Button from "@mui/material/Button";
import Stack from "@mui/material/Stack";
import { useTranslations } from "next-intl";

import { PageContainer } from "@/components/layout/page-container";

/**
 * Último recurso localizado para errores de navegación.
 *
 * No renderiza {@code error.message} ni el digest de Next.js: ambos pueden contener información de
 * infraestructura o de un proveedor. La recuperación explícita vuelve a ejecutar el segmento.
 */
export default function AppError({ reset }: Readonly<{ reset: () => void }>) {
  const t = useTranslations("PublicErrors");
  return (
    <PageContainer component="main" maxWidth="sm" sx={{ py: { xs: 4, md: 8 } }}>
      <Stack spacing={2}>
        <Alert severity="error">
          <strong>{t("title")}</strong>
          <br />
          {t("unavailable")}
        </Alert>
        <Button onClick={reset} variant="contained">
          {t("retry")}
        </Button>
      </Stack>
    </PageContainer>
  );
}
