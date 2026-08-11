import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { LegalDocumentPage, type LegalSection } from "@/features/legal/legal-document-page";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Legal.terms");
  return { title: t("metadata.title"), description: t("metadata.description") };
}

/** Condiciones de uso públicas y bilingües del MVP. */
export default async function TermsPage() {
  const t = await getTranslations("Legal.terms");
  return (
    <LegalDocumentPage
      currentPath="/legal/condiciones"
      description={t("description")}
      relatedHref="/legal/privacidad"
      relatedLabel={t("relatedLabel")}
      relatedText={t("relatedText")}
      reviewNotice={t("reviewNotice")}
      sections={t.raw("sections") as LegalSection[]}
      title={t("title")}
      updatedLabel={t("updatedLabel")}
      updatedValue={t("updatedValue")}
    />
  );
}
