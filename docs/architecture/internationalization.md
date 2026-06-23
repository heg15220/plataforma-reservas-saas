# Internacionalización frontend

## Objetivo

La infraestructura i18n de Reserly usa `next-intl` con catálogos versionados para separar los textos visibles de la estructura de componentes. La resolución de idioma ya sigue el contrato de producto: preferencia guardada, parámetro explícito seguro, idioma de app/navegador y fallback `en`.

## Estructura

```text
apps/web/locales/
  es.json
  en.json
apps/web/proxy.ts
apps/web/src/i18n/
  config.ts
  locale-resolution.ts
  locale-resolution.test.ts
  request.ts
  messages.test.ts
scripts/
  validate-i18n.mjs
```

- `locales/es.json`: catálogo español con tildes, eñes, signos y caracteres UTF-8 correctos.
- `locales/en.json`: catálogo inglés con las mismas claves.
- `config.ts`: locales soportados, fallback, cookie de preferencia y nombres de cabeceras internas.
- `locale-resolution.ts`: resolución pura y testeable de preferencia, parámetros, cabeceras y fallback.
- `request.ts`: configuración request-scoped de `next-intl`, lectura de cookies/cabeceras y carga cerrada de catálogos.
- `proxy.ts`: normaliza `?locale=`/`?lang=`, persiste la preferencia en cookie y reenvía el valor seguro a la request actual.
- `messages.test.ts`: prueba de paridad de claves y caracteres críticos.
- `locale-resolution.test.ts`: prueba de prioridad, sanitización y `Accept-Language`.
- `scripts/validate-i18n.mjs`: validación CI de catálogos completos y ausencia de texto visible hardcodeado en TSX.

## Contratos

- Locales soportados inicialmente: `es` y `en`.
- Fallback visible: `en`.
- Cookie de preferencia: `reserly-locale`, con valores persistidos exactos `es` o `en`.
- Parámetros públicos admitidos: `locale` y `lang`.
- Cabecera interna de parámetro explícito: `x-reserly-locale-param`.
- Cabecera opcional de idioma de app: `x-reserly-app-locale`.
- Cualquier variante segura que empiece por `es`, como `es-ES`, `es-MX` o `es-AR`, resuelve a `es`.
- Cualquier otra variante segura resuelve a `en`.
- Valores inseguros, demasiado largos o con caracteres no permitidos se ignoran antes de tocar cookies o catálogos.
- El layout raíz obtiene `locale` y `messages` desde `next-intl/server` y los inyecta en `NextIntlClientProvider`.
- `next.config.ts` usa el plugin oficial de `next-intl` apuntando a `./src/i18n/request.ts`.

## Orden de resolución

```text
1. Preferencia guardada en cookie `reserly-locale` si es `es` o `en`.
2. Parámetro seguro `?locale=` o `?lang=`, normalizado por `proxy.ts`.
3. Cabecera de app `x-reserly-app-locale` si existe.
4. Cabecera `Accept-Language` del navegador, respetando `q` y orden.
5. Fallback `en`.
```

Cuando un parámetro público seguro aparece en la URL, `proxy.ts` lo normaliza y lo guarda como cookie. También actualiza la cookie de la request actual para que el render en curso use la nueva preferencia sin esperar a la siguiente navegación.

## Reglas de uso

- Las nuevas pantallas deben crear claves bajo un namespace propio.
- Los textos visibles de navegación, botones, estados, ayudas, títulos y errores deben salir del catálogo.
- Las claves deben existir en ambos catálogos antes de integrar.
- `npm run i18n:check` debe pasar antes de cerrar una tarea que toque UI o catálogos.
- Los textos JSX directos y atributos visibles como `aria-label`, `alt`, `title`, `placeholder`, `label` o `helperText` deben provenir de `useTranslations`, `getTranslations` o props ya localizadas.
- Las fechas, horas, números y moneda deberán formatearse con el locale efectivo cuando aparezcan flujos funcionales.
- Los textos configurables guardados en base de datos se definirán en `0.13`; no se resuelven con estos catálogos estáticos.

## Límites actuales

- No hay selector visual de idioma todavía; el cambio manual puede hacerse con `?locale=es`, `?locale=en`, `?lang=es` o `?lang=en`.
- No se ha implementado la ruta pública `GET /api/public/i18n/{locale}`.
- `i18n:check` valida texto visible en archivos `.tsx`; no analiza todavía emails, templates backend, seeds, migraciones o textos dinámicos de base de datos.
- La validación profunda de mojibake y calidad ortográfica corresponde a `0.15`.
