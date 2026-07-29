import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueStatisticsDashboard } from "@/features/venue-statistics/venue-statistics-dashboard";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueStatistics.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Panel privado responsive con métricas agregadas y evolución temporal simple. */
export default async function VenueStatisticsPage() {
  const t = await getTranslations("VenueStatistics.hero");
  return (
    <VenueShell currentPath="/panel/estadisticas">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueStatisticsDashboard />
    </VenueShell>
  );
}
