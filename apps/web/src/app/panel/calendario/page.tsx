import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueInternalCalendar } from "@/features/availability/venue-internal-calendar";
import { VenueAvailabilityManager } from "@/features/availability/venue-availability-manager";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("Availability.private.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Ruta privada para gestionar horario semanal, excepciones y franjas propias. */
export default async function VenueCalendarPage() {
  const t = await getTranslations("Availability.private");

  return (
    <VenueShell currentPath="/panel/calendario">
      <PageHeading
        eyebrow={t("hero.eyebrow")}
        summary={t("hero.summary")}
        title={t("hero.title")}
      />
      <VenueInternalCalendar />
      <VenueAvailabilityManager />
    </VenueShell>
  );
}
