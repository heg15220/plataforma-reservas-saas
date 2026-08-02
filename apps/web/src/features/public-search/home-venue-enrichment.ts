import { fetchPublicAvailability } from "@/features/availability/availability-api";
import { getPublicVenue } from "@/features/public-venue/public-venue-api";

import type { PublicVenueSearchItem } from "./public-search-api";

const BUSINESS_TIME_ZONE = "Europe/Madrid";

/**
 * Completa las tarjetas del inicio con la ficha y disponibilidad públicas ya implementadas.
 *
 * La búsqueda antigua no proyecta calle/código postal y mantiene disponibilidad automática como
 * pendiente. Esta composición SSR tolera despliegues escalonados y degrada por local: un fallo de
 * ficha o disponibilidad conserva los datos originales sin ocultar el resto del catálogo.
 */
export async function enrichHomeVenueCards(
  venues: PublicVenueSearchItem[],
  locale: string,
  now = new Date(),
): Promise<PublicVenueSearchItem[]> {
  const date = businessDate(now);

  return Promise.all(
    venues.map(async (venue) => {
      const [profileResult, availabilityResult] = await Promise.allSettled([
        getPublicVenue(venue.slug, locale),
        fetchPublicAvailability(venue.slug, date, locale),
      ]);
      const profile = profileResult.status === "fulfilled" ? profileResult.value : null;
      const availability =
        availabilityResult.status === "fulfilled" ? availabilityResult.value : null;
      const open = availability?.statusCode === "open";

      return {
        ...venue,
        address: profile?.address ?? venue.address,
        postalCode: profile?.postalCode ?? venue.postalCode,
        city: profile?.city ?? venue.city,
        province: profile?.province ?? venue.province,
        country: profile?.country ?? venue.country,
        ...(availability
          ? {
              availabilitySummary: availability.statusLabel,
              bookingAvailable: availability.bookingAvailable,
              statusCode: open ? ("available" as const) : ("unavailable" as const),
              statusLabel: availability.statusLabel,
            }
          : {}),
      };
    }),
  );
}

/** Devuelve YYYY-MM-DD con el mismo huso horario de negocio que el backend. */
function businessDate(now: Date): string {
  const parts = new Intl.DateTimeFormat("en", {
    day: "2-digit",
    month: "2-digit",
    timeZone: BUSINESS_TIME_ZONE,
    year: "numeric",
  }).formatToParts(now);
  const value = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${value.year}-${value.month}-${value.day}`;
}
