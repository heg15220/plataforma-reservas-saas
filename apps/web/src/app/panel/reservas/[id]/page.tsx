import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueReservationDetailPanel } from "@/features/venue-reservations/venue-reservation-detail-panel";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueReservations.detail.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Detalle privado responsive de una reserva perteneciente al local autenticado. */
export default async function VenueReservationDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const [{ id }, t] = await Promise.all([
    params,
    getTranslations("VenueReservations.detail.hero"),
  ]);

  return (
    <VenueShell currentPath="/panel/reservas">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <VenueReservationDetailPanel reservationId={id} />
    </VenueShell>
  );
}
