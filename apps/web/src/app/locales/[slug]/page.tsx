import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getLocale, getTranslations } from "next-intl/server";

import { getPublicVenue, PublicVenueNotFoundError } from "@/features/public-venue/public-venue-api";
import { PublicVenueProfileView } from "@/features/public-venue/public-venue-profile";

interface VenuePageProps {
  params: Promise<{ slug: string }>;
}

/** Metadatos localizados y derivados únicamente de perfiles efectivamente publicados. */
export async function generateMetadata({ params }: VenuePageProps): Promise<Metadata> {
  const { slug } = await params;
  const locale = await getLocale();
  try {
    const venue = await getPublicVenue(slug, locale);
    return { title: venue.name, description: venue.description ?? venue.categoryName };
  } catch (error) {
    if (error instanceof PublicVenueNotFoundError) {
      const t = await getTranslations("VenuePublicProfile.metadata");
      return { title: t("notFoundTitle") };
    }
    throw error;
  }
}

/** Ruta pública por slug; oculta con 404 cualquier local que no esté publicado. */
export default async function VenuePage({ params }: VenuePageProps) {
  const { slug } = await params;
  const locale = await getLocale();
  const venue = await loadPublishedVenue(slug, locale);
  return <PublicVenueProfileView venue={venue} />;
}

async function loadPublishedVenue(slug: string, locale: string) {
  try {
    return await getPublicVenue(slug, locale);
  } catch (error) {
    if (error instanceof PublicVenueNotFoundError) {
      notFound();
    }
    throw error;
  }
}
