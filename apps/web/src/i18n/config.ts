export const supportedLocales = ["es", "en"] as const;

export type SupportedLocale = (typeof supportedLocales)[number];
export type Messages = typeof import("../../locales/en.json");

export const defaultLocale: SupportedLocale = "es";
export const fallbackLocale: SupportedLocale = "en";

export function isSupportedLocale(value: string): value is SupportedLocale {
  return supportedLocales.includes(value as SupportedLocale);
}
