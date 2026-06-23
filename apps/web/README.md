# Web de Reserly

Aplicación Next.js 16 con React 19, TypeScript, App Router y Material UI para la web pública, el panel de locales y el panel de administración.

La aplicación mantiene una única base para compartir navegación, diseño, accesibilidad e internacionalización.

La infraestructura responsive incluye:

- `PublicShell`: cabecera de escritorio y navegación inferior móvil.
- `VenueShell`: sidebar persistente en escritorio y navegación inferior móvil.
- `PageContainer`, `PageHeading`, `ResponsiveGrid` y `Surface`: primitivas de composición.
- `AppProviders`: integración SSR de MUI y Emotion para Next.js 16.

La ruta `/panel-preview` permite revisar temporalmente el shell privado sin datos y está marcada como `noindex`.

El tema actual es estructural y provisional. Los tokens visuales, estados, tipografía completa e iconografía se formalizarán en `0.8`. La resolución dinámica de idioma sustituirá el `lang="es"` temporal en `0.11`.

Las variables se validan durante el arranque y el build mediante `environment.ts`. Solo `NEXT_PUBLIC_APP_ENV` y `NEXT_PUBLIC_API_BASE_URL` pueden exponerse al navegador.

## Ejecución

```bash
npm run dev:web
```

El comando anterior se ejecuta desde la raíz del monorepo.

## Verificación

```bash
npm run lint --workspace @reserly/web
npm run typecheck --workspace @reserly/web
npm run test --workspace @reserly/web
npm run build --workspace @reserly/web
```
