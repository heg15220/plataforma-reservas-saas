import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { LegalDocumentPage, type LegalSection } from "@/features/legal/legal-document-page";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Legal.privacy");
  return { title: t("metadata.title"), description: t("metadata.description") };
}

/** Política de privacidad pública y bilingüe del MVP. */
export default async function PrivacyPolicyPage() {
  const t = await getTranslations("Legal.privacy");
  return (
    <LegalDocumentPage
      currentPath="/legal/privacidad"
      description={t("description")}
      relatedHref="/legal/condiciones"
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
