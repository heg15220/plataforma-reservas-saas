# Calidad de textos españoles

## Objetivo

Reserly debe conservar textos españoles legibles y correctos en todos los puntos visibles para usuarios, locales, administradores y documentación de operación. La validación automatizada de esta guía cubre la tarea `0.15` y convierte `RNF-012` en una comprobación de CI.

Comando principal:

```bash
npm run spanish:text:check
```

El comando ejecuta `scripts/validate-spanish-text.mjs` y forma parte de `npm run verify`.

## Alcance

El validador revisa archivos de texto versionados del repositorio y aplica dos niveles:

- Validación UTF-8 y mojibake sobre archivos de texto relevantes del monorepo.
- Validación lingüística básica sobre textos españoles visibles o documentales.

El alcance lingüístico incluye:

- Catálogo español `apps/web/locales/es.json`.
- Documentación en `README.md`, `docs/**/*.md` y `.kiro/specs/**/*.md`.
- Plantillas `.env.*.example` con comentarios visibles para desarrollo.
- Migraciones y recursos backend bajo `apps/api/src/main/resources`.
- Futuras carpetas de emails, plantillas, seeds o fixtures cuando existan.

## Reglas Automatizadas

### UTF-8 estricto

Cada archivo escaneado debe poder decodificarse como UTF-8 válido. Si un archivo contiene bytes inválidos, el comando falla indicando la ruta.

### Mojibake

Se detectan secuencias típicas de texto UTF-8 leído con una codificación incorrecta, por ejemplo:

- caracteres que empiezan por `\u00c3`;
- caracteres que empiezan por `\u00c2`;
- caracteres que empiezan por `\u00e2`;
- secuencias de reemplazo Unicode.

Los ejemplos de mojibake escritos dentro de código inline Markdown no se tratan como error para permitir documentar el problema sin romper la validación.

### Signos de apertura

Si una línea parece texto español y contiene una pregunta o exclamación visible, debe usar `¿` o `¡` respectivamente.

Correcto:

```text
¿Dónde quieres pedir cita hoy?
¡Reserva confirmada!
```

Incorrecto:

```text
Donde quieres pedir cita hoy?
Reserva confirmada!
```

### Tildes frecuentes

El script mantiene una lista pequeña de palabras habituales del proyecto que no deben aparecer sin tilde o sin `ñ` en texto español visible, como:

- `configuración`;
- `validación`;
- `catálogo`;
- `español`;
- `inglés`;
- `público`;
- `móvil`;
- `búsqueda`;
- `verificación`;
- `asíncrono`.

La lista es intencionalmente conservadora. Su objetivo es detectar errores repetidos sin sustituir una revisión humana completa.

## Relación con i18n

`npm run i18n:check` valida que:

- los catálogos `es` y `en` tienen las mismas claves;
- los componentes TSX no introducen texto visible hardcodeado;
- las claves usadas por `useTranslations` y `getTranslations` existen.

`npm run spanish:text:check` valida que el contenido español ya versionado conserva codificación y calidad mínima. Ambos checks se complementan.

## Cómo Corregir Fallos

Cuando el script falle:

1. Abrir el archivo indicado con un editor configurado en UTF-8.
2. Corregir mojibake real, tildes ausentes o signos de apertura.
3. Evitar normalizar textos visibles eliminando tildes para facilitar comparaciones.
4. Si el texto está dentro de una muestra técnica, envolverlo en código inline o bloque Markdown solo cuando realmente sea un ejemplo.
5. Ejecutar de nuevo `npm run spanish:text:check`.

## Límites

La validación no es un corrector ortográfico completo. No detecta todos los errores gramaticales ni todas las tildes ausentes posibles. La revisión humana sigue siendo obligatoria para textos de usuario final, emails, estados públicos, textos legales y documentación publicada.

Cuando se añadan plantillas de email, seeds con datos visibles, fixtures de aceptación o documentación de usuario, deben permanecer bajo rutas incluidas por el validador o ampliarse los patrones de `scripts/validate-spanish-text.mjs`.
