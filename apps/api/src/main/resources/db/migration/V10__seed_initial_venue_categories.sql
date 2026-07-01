-- Inserta el catálogo mínimo de categorías de la Fase 2.
--
-- Los UUID y slugs son estables para que fixtures, URLs, filtros y futuras
-- migraciones puedan referenciar estas categorías sin depender del orden de
-- inserción. Los nombres localizados satisfacen el contrato estructural de V9;
-- la tarea 2.3 realizará la revisión específica de traducciones, fallback y
-- completitud del catálogo.

INSERT INTO "Categories" (
  "id",
  "name",
  "nameI18n",
  "slug",
  "isActive"
)
VALUES
  (
    '20000000-0000-0000-0000-000000000001',
    'Restaurante',
    '{"sourceLocale":"es","values":{"es":"Restaurante","en":"Restaurant"}}'::jsonb,
    'restaurante',
    true
  ),
  (
    '20000000-0000-0000-0000-000000000002',
    'Peluquería',
    '{"sourceLocale":"es","values":{"es":"Peluquería","en":"Hair salon"}}'::jsonb,
    'peluqueria',
    true
  ),
  (
    '20000000-0000-0000-0000-000000000003',
    'Campo de fútbol',
    '{"sourceLocale":"es","values":{"es":"Campo de fútbol","en":"Football pitch"}}'::jsonb,
    'campo-de-futbol',
    true
  ),
  (
    '20000000-0000-0000-0000-000000000004',
    'Pista de pádel',
    '{"sourceLocale":"es","values":{"es":"Pista de pádel","en":"Padel court"}}'::jsonb,
    'pista-de-padel',
    true
  ),
  (
    '20000000-0000-0000-0000-000000000005',
    'Instalación municipal',
    '{"sourceLocale":"es","values":{"es":"Instalación municipal","en":"Municipal facility"}}'::jsonb,
    'instalacion-municipal',
    true
  ),
  (
    '20000000-0000-0000-0000-000000000006',
    'Centro deportivo',
    '{"sourceLocale":"es","values":{"es":"Centro deportivo","en":"Sports center"}}'::jsonb,
    'centro-deportivo',
    true
  ),
  (
    '20000000-0000-0000-0000-000000000007',
    'Centro de estética',
    '{"sourceLocale":"es","values":{"es":"Centro de estética","en":"Beauty center"}}'::jsonb,
    'centro-de-estetica',
    true
  ),
  (
    '20000000-0000-0000-0000-000000000008',
    'Otros',
    '{"sourceLocale":"es","values":{"es":"Otros","en":"Other"}}'::jsonb,
    'otros',
    true
  );
