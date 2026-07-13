import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { ReservationFormManager } from "@/features/reservation-form/reservation-form-manager";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("FormBuilder.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Ruta privada para configurar y previsualizar el formulario de reserva del local. */
export default async function ReservationFormPage() {
  const t = await getTranslations("FormBuilder.hero");

  return (
    <VenueShell currentPath="/panel/formulario">
      <PageHeading eyebrow={t("eyebrow")} summary={t("summary")} title={t("title")} />
      <ReservationFormManager />
    </VenueShell>
  );
}
