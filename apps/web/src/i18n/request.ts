import { cookies, headers } from "next/headers";
import { getRequestConfig } from "next-intl/server";

import {
  appLocaleHeaderName,
  explicitLocaleHeaderName,
  localeCookieName,
  type Messages,
  type SupportedLocale,
} from "./config";
import { resolveEffectiveLocale } from "./locale-resolution";

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
 * Resuelve el idioma efectivo con la prioridad de producto: preferencia
 * guardada, parámetro seguro normalizado por Proxy, idioma de app/navegador y
 * fallback `en`. La carga de mensajes sigue usando un mapa cerrado por locale.
 */
export default getRequestConfig(async () => {
  const requestHeaders = await headers();
  const requestCookies = await cookies();
  const { locale } = resolveEffectiveLocale({
    savedPreference: requestCookies.get(localeCookieName)?.value,
    explicitLocale: requestHeaders.get(explicitLocaleHeaderName),
    appLocale: requestHeaders.get(appLocaleHeaderName),
    acceptLanguage: requestHeaders.get("accept-language"),
  });

  return {
    locale,
    messages: await loadMessages(locale),
  };
});
