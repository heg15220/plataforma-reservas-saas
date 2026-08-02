import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueEmailManager } from "@/features/venue-emails/venue-email-manager";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueEmails.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Configuración privada de destinatarios de reservas por local publicado. */
export default async function VenueEmailsPage() {
  const t = await getTranslations("VenueEmails.hero");
  return (
    <VenueShell currentPath="/panel/emails">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueEmailManager />
    </VenueShell>
  );
}
