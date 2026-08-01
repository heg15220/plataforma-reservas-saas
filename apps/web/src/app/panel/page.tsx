import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueDashboardOverview } from "@/features/venue-dashboard/venue-dashboard-overview";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueDashboard.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/**
 * Punto de entrada estable al panel operativo con resumen del día.
 */
export default async function VenuePanelPage() {
  const t = await getTranslations("VenueDashboard.hero");

  return (
    <VenueShell currentPath="/panel">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueDashboardOverview />
    </VenueShell>
  );
}
