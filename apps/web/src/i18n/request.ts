import { getRequestConfig } from "next-intl/server";

import {
  defaultLocale,
  fallbackLocale,
  isSupportedLocale,
  type Messages,
  type SupportedLocale,
} from "./config";

const localeLoaders = {
  en: () => import("../../locales/en.json"),
  es: () => import("../../locales/es.json"),
} satisfies Record<SupportedLocale, () => Promise<{ default: Messages }>>;

export async function loadMessages(locale: SupportedLocale): Promise<Messages> {
  return (await localeLoaders[locale]()).default;
}

/**
 * Configuración request-scoped de next-intl.
 *
 * La tarea 0.10 crea la infraestructura y usa un locale estático para conservar
 * la interfaz actual. La resolución por preferencia, parámetro seguro,
 * navegador/app y fallback queda aislada para la tarea 0.11.
 */
export default getRequestConfig(async () => {
  const locale = isSupportedLocale(defaultLocale) ? defaultLocale : fallbackLocale;

  return {
    locale,
    messages: await loadMessages(locale),
  };
});
