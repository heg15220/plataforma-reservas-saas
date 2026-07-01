import Alert from "@mui/material/Alert";
import Stack from "@mui/material/Stack";
import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { BusinessDocumentUpload } from "@/features/business-documents/business-document-upload";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("BusinessDocuments.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/**
 * Portal privado para satisfacer una solicitud de respaldo empresarial.
 *
 * Los datos se cargan en cliente con la cookie HttpOnly; la página estática no
 * recibe ni serializa documentos, identificadores fiscales o IDs de cuenta.
 */
export default async function BusinessVerificationDocumentsPage() {
  const t = await getTranslations("BusinessDocuments");

  return (
    <VenueShell currentPath="/panel/mas">
      <Stack spacing={{ xs: 5, md: 6 }}>
        <PageHeading
          eyebrow={t("hero.eyebrow")}
          summary={t("hero.summary")}
          title={t("hero.title")}
        />
        <Alert severity="info">{t("hero.reviewTime")}</Alert>
        <BusinessDocumentUpload />
      </Stack>
    </VenueShell>
  );
}
