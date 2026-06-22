# Web de Reserly

Aplicación Next.js con TypeScript y App Router para la web pública, el panel de locales y el panel de administración.

El esqueleto inicial mantiene una única aplicación para compartir navegación, diseño, accesibilidad e internacionalización. Las rutas y componentes se organizarán por área funcional a medida que se implementen las tareas.

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
