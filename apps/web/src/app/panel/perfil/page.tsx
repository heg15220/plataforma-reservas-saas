import type { Metadata } from "next";
import { getTranslations } from "next-intl/server";

import { PageHeading, VenueShell } from "@/components/layout";
import { VenueProfileEditor } from "@/features/venue-profile/venue-profile-editor";

export async function generateMetadata(): Promise<Metadata> {
  const t = await getTranslations("VenueProfileEditor.metadata");
  return {
    title: t("title"),
    description: t("description"),
    robots: { index: false, follow: false },
  };
}

/** Página privada del propietario para configurar la ficha pública del local. */
export default async function VenueProfileEditorPage() {
  const t = await getTranslations("VenueProfileEditor");

  return (
    <VenueShell currentPath="/panel/perfil">
      <PageHeading
        eyebrow={t("hero.eyebrow")}
        summary={t("hero.summary")}
        title={t("hero.title")}
      />
      <VenueProfileEditor />
    </VenueShell>
  );
}
