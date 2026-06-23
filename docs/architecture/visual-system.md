# Sistema visual de Reserly

## Objetivo

El sistema visual convierte la dirección definida en `.kiro` en contratos reutilizables de código. Colores, tipografía, radios, sombras, estados e iconos deben consumirse desde esta capa para evitar variaciones arbitrarias entre pantallas.

La ruta interna `/design-system`, marcada como `noindex`, actúa como catálogo vivo y permite revisar los fundamentos dentro del runtime real de Next.js y Material UI.

## Tokens

`apps/web/src/theme/visual-tokens.ts` es la fuente de verdad para valores visuales propios:

- marca: azul principal, hover y fondo tonal;
- texto: principal, secundario e inverso;
- superficies: página, tarjeta, elevada e inversa;
- bordes: normal y reforzado;
- estados: éxito, advertencia, peligro, neutral e información;
- radios: controles, tarjetas, paneles y formas redondas;
- sombras: tarjeta y elemento flotante;
- familia y pesos tipográficos.

Los componentes deben preferir aliases del tema MUI cuando existan. El acceso directo a `visualTokens` queda reservado para conceptos que MUI no representa de forma suficiente, como fondos tonales de estado o el sidebar inverso.

## Tema Material UI

`base-theme.ts` traduce los tokens a:

- `palette`, `background`, `text` y `divider`;
- escala de espaciado de `4 px`;
- variantes `h1`, `h2`, `h3`, cuerpo, botón y overline;
- radio base de controles;
- estados de botones, campos, chips, superficies y tooltips;
- foco visible de alto contraste;
- controles táctiles de al menos `44 px`.

La tipografía declara `Inter` como primera opción y usa fallbacks del sistema. No se descarga una fuente remota durante el build; una futura incorporación de archivos propios deberá mantener rendimiento, privacidad y compatibilidad con CSP.

## Estados

`StatusChip` ofrece cinco tonos:

- `success`;
- `warning`;
- `danger`;
- `neutral`;
- `info`.

Cada estado combina texto, icono y color. El icono es decorativo porque la etiqueta contiene el significado completo. El texto debe llegar ya localizado desde el consumidor.

Los colores suaves usan un tono de texto más oscuro que el color de marca del estado para mantener contraste WCAG 2.2 AA.

## Iconografía

La biblioteca fijada es `lucide-react`. Se usa un trazo lineal coherente, normalmente entre `1.9` y `2.2`.

- Iconos acompañados por texto: `aria-hidden="true"`.
- Botones exclusivamente iconográficos: nombre accesible obligatorio mediante `aria-label` o texto equivalente.
- Iconos informativos independientes: deben exponer un nombre accesible.
- No se usan iconos como único indicador de estado crítico.

El isotipo actual de Reserly combina un calendario con una confirmación. Es vectorial, legible en tamaños compactos y dispone de variantes normal e inversa.

## Responsive y accesibilidad

- El catálogo se valida desde `320 px`.
- Los controles mantienen un objetivo táctil mínimo de `44 px`.
- El zoom al `200 %` no debe ocultar contenido ni provocar desbordamiento horizontal.
- `prefers-reduced-motion` reduce transiciones y animaciones.
- El foco visible usa el azul de marca con separación exterior.
- Los textos largos deben poder envolver sin truncar estados ni acciones esenciales.
