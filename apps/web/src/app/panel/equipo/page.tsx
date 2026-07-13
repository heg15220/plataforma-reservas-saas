import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { TeamAvailabilityManager } from "@/features/team/team-availability-manager";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Team.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Ruta privada para gestionar recursos, horarios y compatibilidades de servicios. */
export default async function TeamPage() {
  const t = await getTranslations("Team.hero");

  return (
    <VenueShell currentPath="/panel/equipo">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <TeamAvailabilityManager />
    </VenueShell>
  );
}