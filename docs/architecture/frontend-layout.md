# Layout responsive del frontend

## Objetivo

La base de interfaz separa navegación, composición y contenido funcional. Las pantallas futuras deben reutilizar estos contratos en vez de crear márgenes, cabeceras, sidebars o grids propios.

## Integración con Next.js

`AppProviders` envuelve el App Router con:

- `AppRouterCacheProvider` de `@mui/material-nextjs/v16-appRouter`;
- `ThemeProvider`;
- `CssBaseline`.

El cache provider recoge el CSS generado durante el streaming SSR y lo inserta en `head`. `enableCssLayer` permite que los estilos globales y los futuros CSS Modules tengan una precedencia explícita frente a MUI.

`NavigationLink` es un adaptador cliente de `next/link`. Next.js 16 no permite pasar directamente determinadas funciones de Server Components a la prop `component` de MUI; el adaptador ofrece un límite cliente estable.

## Shell público

`PublicShell` aporta:

- enlace de salto al contenido;
- cabecera sticky;
- identidad Reserly;
- navegación horizontal desde `md` (`900 px`);
- acceso para locales;
- navegación inferior con cinco destinos por debajo de `md`;
- padding inferior que evita ocultar contenido tras la navegación fija;
- landmark `main` estable.

La prop `currentPath` aplica `aria-current="page"` a la ruta activa. Cuando se implemente navegación real dependiente de URL, un componente cliente podrá obtener el pathname y delegarlo al shell.

## Shell del panel

`VenueShell` aporta:

- enlace de salto al contenido;
- sidebar fijo de `256 px` desde `md`;
- nombre del local;
- navegación lateral accesible;
- cabecera compacta en móvil;
- navegación inferior de cuatro destinos;
- contenido centrado con ancho máximo de `1120 px`;
- desplazamiento del contenido equivalente al ancho del sidebar.

En móvil no se trasladan tablas ni navegación lateral: las pantallas deben componer tarjetas y listas dentro del mismo shell.

## Primitivas

### `PageContainer`

Centra contenido, limita el ancho a `1440 px` o `1120 px` en modo compacto y aplica gutters fluidos.

### `PageHeading`

Agrupa eyebrow, título, resumen y acciones. En móvil apila la acción y permite que ocupe todo el ancho; en escritorio alinea texto y acción.

### `ResponsiveGrid`

Usa `auto-fit` y `minmax` para adaptar tarjetas al espacio disponible sin acoplar cada listado a breakpoints concretos.

### `Surface`

Proporciona borde, radio y padding consistentes para `section`, `article` o `aside`. El elemento semántico debe elegirse según el contenido.

### `Brand`

Combina el wordmark Reserly con un isotipo vectorial de calendario y confirmación. Dispone de variantes compacta e inversa.

## Accesibilidad

- Todos los controles táctiles principales miden al menos `44 px`.
- Los shells incluyen enlaces de salto.
- Las rutas activas usan `aria-current`.
- Los landmarks y nombres de navegación diferencian escritorio y móvil.
- `focus-visible` aplica un anillo perceptible.
- `prefers-reduced-motion` reduce animaciones y transiciones.
- El ancho mínimo verificado es `320 px`.
- El documento ya recibe `lang` desde `next-intl`. En `0.10` el locale efectivo es estático para conservar la interfaz actual; la resolución dinámica por preferencia, parámetro seguro, navegador/app y fallback se implementará en `0.11`.

## Sistema visual

La tarea `0.8` amplía esta infraestructura con tokens semánticos, tema MUI, estados accesibles, iconografía Lucide y el catálogo `/design-system`. La definición completa se encuentra en `visual-system.md`.

La página inicial y `/panel-preview` son demostraciones estructurales. No sustituyen el buscador público, el dashboard ni los datos reales de sus tareas funcionales.
