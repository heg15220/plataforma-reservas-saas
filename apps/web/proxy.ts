import { NextResponse, type NextRequest } from "next/server";

import {
  explicitLocaleHeaderName,
  localeCookieMaxAgeSeconds,
  localeCookieName,
} from "@/i18n/config";
import { resolveLocaleTag } from "@/i18n/locale-resolution";

/**
 * Proxy de idioma.
 *
 * Captura parámetros públicos acotados (`locale` o `lang`), los normaliza a un
 * locale soportado y los persiste como preferencia de navegación. La cookie se
 * inyecta también en la request actual para que `next-intl` aplique el cambio
 * en el mismo render sin abrir rutas localizadas.
 */
export function proxy(request: NextRequest) {
  const requestHeaders = new Headers(request.headers);
  const explicitLocale = resolveLocaleTag(
    readExplicitLocaleParameter(request.nextUrl.searchParams),
  );

  if (explicitLocale) {
    requestHeaders.set(explicitLocaleHeaderName, explicitLocale);
    request.cookies.set(localeCookieName, explicitLocale);
    requestHeaders.set("cookie", request.cookies.toString());
  }

  const response = NextResponse.next({
    request: {
      headers: requestHeaders,
    },
  });

  if (explicitLocale) {
    response.cookies.set({
      name: localeCookieName,
      value: explicitLocale,
      maxAge: localeCookieMaxAgeSeconds,
      path: "/",
      sameSite: "lax",
      secure: request.nextUrl.protocol === "https:",
    });
  }

  return response;
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|robots.txt|sitemap.xml|.*\\..*).*)"],
};

function readExplicitLocaleParameter(searchParams: URLSearchParams): string | null {
  return searchParams.get("locale") ?? searchParams.get("lang");
}
