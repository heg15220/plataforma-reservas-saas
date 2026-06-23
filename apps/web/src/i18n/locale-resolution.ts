import { fallbackLocale, isSupportedLocale, type SupportedLocale } from "./config";

export type LocaleResolutionSource =
  | "saved-preference"
  | "explicit-parameter"
  | "app-locale"
  | "accept-language"
  | "fallback";

export type LocaleResolutionInput = {
  savedPreference?: string | null;
  explicitLocale?: string | null;
  appLocale?: string | null;
  acceptLanguage?: string | null;
};

export type LocaleResolution = {
  locale: SupportedLocale;
  source: LocaleResolutionSource;
};

const safeLocaleTagPattern = /^[a-z]{2,3}(?:-[a-z0-9]{2,8}){0,3}$/i;
const maxLocaleTagLength = 32;

/**
 * Normaliza una preferencia ya persistida por el sistema.
 *
 * Las preferencias guardadas solo aceptan locales soportados exactos porque
 * son datos internos; variantes regionales se resuelven antes de persistirse.
 */
export function resolveSavedLocale(value?: string | null): SupportedLocale | undefined {
  const normalizedValue = value?.trim().toLowerCase();

  if (normalizedValue && isSupportedLocale(normalizedValue)) {
    return normalizedValue;
  }

  return undefined;
}

/**
 * Valida un tag BCP 47 acotado para parámetros públicos y cabeceras internas.
 *
 * No intenta aceptar toda la especificación BCP 47. El objetivo aquí es evitar
 * que valores arbitrarios lleguen a cookies, cabeceras o carga de catálogos.
 */
export function readSafeLocaleTag(value?: string | null): string | undefined {
  const normalizedValue = value?.trim();

  if (!normalizedValue || normalizedValue.length > maxLocaleTagLength) {
    return undefined;
  }

  if (!safeLocaleTagPattern.test(normalizedValue)) {
    return undefined;
  }

  return normalizedValue.toLowerCase();
}

export function resolveLocaleTag(value?: string | null): SupportedLocale | undefined {
  const safeTag = readSafeLocaleTag(value);

  if (!safeTag) {
    return undefined;
  }

  return safeTag.startsWith("es") ? "es" : fallbackLocale;
}

export function resolveAcceptLanguageLocale(value?: string | null): SupportedLocale | undefined {
  const ranges = parseAcceptLanguage(value);
  const preferredRange = ranges.find((range) => range.quality > 0);

  if (!preferredRange) {
    return undefined;
  }

  if (preferredRange.tag === "*") {
    return fallbackLocale;
  }

  return resolveLocaleTag(preferredRange.tag);
}

export function resolveEffectiveLocale(input: LocaleResolutionInput): LocaleResolution {
  const savedPreference = resolveSavedLocale(input.savedPreference);

  if (savedPreference) {
    return { locale: savedPreference, source: "saved-preference" };
  }

  const explicitLocale = resolveLocaleTag(input.explicitLocale);

  if (explicitLocale) {
    return { locale: explicitLocale, source: "explicit-parameter" };
  }

  const appLocale = resolveLocaleTag(input.appLocale);

  if (appLocale) {
    return { locale: appLocale, source: "app-locale" };
  }

  const acceptedLocale = resolveAcceptLanguageLocale(input.acceptLanguage);

  if (acceptedLocale) {
    return { locale: acceptedLocale, source: "accept-language" };
  }

  return { locale: fallbackLocale, source: "fallback" };
}

type AcceptLanguageRange = {
  tag: string;
  quality: number;
  order: number;
};

function parseAcceptLanguage(value?: string | null): AcceptLanguageRange[] {
  if (!value) {
    return [];
  }

  return value
    .split(",")
    .map((part, order) => parseAcceptLanguageRange(part, order))
    .filter((range): range is AcceptLanguageRange => range !== undefined)
    .sort((left, right) => right.quality - left.quality || left.order - right.order);
}

function parseAcceptLanguageRange(value: string, order: number): AcceptLanguageRange | undefined {
  const [rawTag, ...parameters] = value.split(";").map((part) => part.trim());
  const tag = rawTag === "*" ? "*" : readSafeLocaleTag(rawTag);

  if (!tag) {
    return undefined;
  }

  const qualityParameter = parameters.find((parameter) => parameter.toLowerCase().startsWith("q="));
  const quality = qualityParameter ? Number(qualityParameter.slice(2)) : 1;

  if (!Number.isFinite(quality) || quality < 0 || quality > 1) {
    return undefined;
  }

  return { tag, quality, order };
}
