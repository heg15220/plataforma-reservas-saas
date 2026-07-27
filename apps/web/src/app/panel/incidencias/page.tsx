import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueIncidentsDashboard } from "@/features/venue-incidents/venue-incidents-dashboard";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueIncidents.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Sección privada responsive de reglas e historial profesional. */
export default async function VenueIncidentsPage({
  searchParams,
}: {
  searchParams: Promise<{ reservationId?: string }>;
}) {
  const [{ reservationId }, t] = await Promise.all([
    searchParams,
    getTranslations("VenueIncidents.hero"),
  ]);
  return (
    <VenueShell currentPath="/panel/incidencias">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueIncidentsDashboard reservationId={reservationId} />
    </VenueShell>
  );
}
