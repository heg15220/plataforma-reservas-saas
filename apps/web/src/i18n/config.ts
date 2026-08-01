export const supportedLocales = ["es", "en"] as const;

export type SupportedLocale = (typeof supportedLocales)[number];
export type Messages = typeof import("../../locales/en.json");

export const defaultLocale: SupportedLocale = "en";
export const fallbackLocale: SupportedLocale = "en";
/** Zona neutra compartida por SSR e hidratación; las fechas de dominio se formatean sin desplazamiento. */
export const defaultTimeZone = "UTC";
export const localeCookieName = "reserly-locale";
export const localeCookieMaxAgeSeconds = 60 * 60 * 24 * 365;
export const explicitLocaleHeaderName = "x-reserly-locale-param";
export const appLocaleHeaderName = "x-reserly-app-locale";

export function isSupportedLocale(value: string): value is SupportedLocale {
  return supportedLocales.includes(value as SupportedLocale);
}
