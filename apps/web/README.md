# Web de Reserly

Aplicación Next.js 16 con React 19, TypeScript, App Router y Material UI para la web pública, el panel de locales y el panel de administración.

La aplicación mantiene una única base para compartir navegación, diseño, accesibilidad e internacionalización.

La infraestructura responsive incluye:

- `PublicShell`: cabecera de escritorio y navegación inferior móvil.
- `VenueShell`: sidebar persistente en escritorio y navegación inferior móvil.
- `PageContainer`, `PageHeading`, `ResponsiveGrid` y `Surface`: primitivas de composición.
- `AppProviders`: integración SSR de MUI y Emotion para Next.js 16.
- `visualTokens` y `baseTheme`: fuente semántica de colores, tipografía, espaciado y estados.
- `StatusChip`: estados accesibles con texto, color e icono.

Las rutas internas `/panel-preview` y `/design-system` permiten revisar el shell privado y el catálogo visual sin datos; ambas están marcadas como `noindex`.

La iconografía usa `lucide-react`. La resolución dinámica de idioma sustituirá el `lang="es"` temporal en `0.11`.

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
