import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueReservationsWorkspace } from "@/features/venue-reservations/venue-reservations-workspace";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueReservations.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Espacio privado unificado para agenda, calendario y disponibilidad profesional. */
export default async function VenueReservationsPage({
  searchParams,
}: {
  searchParams: Promise<{ date?: string }>;
}) {
  const { date } = await searchParams;
  const t = await getTranslations("VenueReservations.hero");
  const initialDate = date && /^\d{4}-\d{2}-\d{2}$/.test(date) ? date : undefined;

  return (
    <VenueShell currentPath="/panel/reservas">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueReservationsWorkspace initialDate={initialDate} />
    </VenueShell>
  );
}
