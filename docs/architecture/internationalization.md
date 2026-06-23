# Internacionalización frontend

## Objetivo

La infraestructura i18n de Reserly usa `next-intl` con catálogos versionados para separar los textos visibles de la estructura de componentes. La tarea `0.10` crea la base técnica; la resolución dinámica por preferencia, parámetro seguro, navegador/app y fallback queda para `0.11`.

## Estructura

```text
apps/web/locales/
  es.json
  en.json
apps/web/src/i18n/
  config.ts
  request.ts
  messages.test.ts
```

- `locales/es.json`: catálogo español con tildes, eñes, signos y caracteres UTF-8 correctos.
- `locales/en.json`: catálogo inglés con las mismas claves.
- `config.ts`: locales soportados, locale estático actual y fallback operativo.
- `request.ts`: configuración request-scoped de `next-intl`.
- `messages.test.ts`: prueba de paridad de claves y caracteres críticos.

## Contratos

- Locales soportados inicialmente: `es` y `en`.
- Locale estático temporal: `es`, para conservar la interfaz actual hasta `0.11`.
- Fallback declarado: `en`.
- Los componentes actuales obtienen texto mediante `useTranslations`.
- El layout raíz obtiene `locale` y `messages` desde `next-intl/server` y los inyecta en `NextIntlClientProvider`.
- `next.config.ts` usa el plugin oficial de `next-intl` apuntando a `./src/i18n/request.ts`.

## Reglas de uso

- Las nuevas pantallas deben crear claves bajo un namespace propio.
- Los textos visibles de navegación, botones, estados, ayudas, títulos y errores deben salir del catálogo.
- Las claves deben existir en ambos catálogos antes de integrar.
- Las fechas, horas, números y moneda deberán formatearse con el locale efectivo cuando aparezcan flujos funcionales.
- Los textos configurables guardados en base de datos se definirán en `0.13`; no se resuelven con estos catálogos estáticos.

## Límites actuales

- No hay selector de idioma ni lectura de cabeceras todavía.
- No se ha implementado la ruta pública `GET /api/public/i18n/{locale}`.
- La detección automática de textos hardcodeados corresponde a `0.12`.
- La validación profunda de mojibake y calidad ortográfica corresponde a `0.15`.
