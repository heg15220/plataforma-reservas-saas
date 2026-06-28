# Normalización y validación local de identificadores empresariales

## Objetivo y frontera

El paquete `businessverification.validation` convierte el país fiscal y el identificador aportado
en una clave canónica antes de consultar unicidad o persistir una cuenta empresarial. Cuando existe
una regla nacional implementada, valida además formato y carácter de control.

Esta validación es local y determinista. No confirma:

- que el identificador haya sido emitido o siga activo;
- que pertenezca a la razón social aportada;
- que su titular esté de alta censal;
- que esté inscrito en el ROI o sea válido en VIES;
- que una empresa pueda publicar locales.

Esas comprobaciones pertenecen a adaptadores remotos, revisión administrativa y máquina de estados
de las tareas `1.6` a `1.11`.

## Contrato de dominio

`BusinessTaxIdentifierValidationService.normalizeAndValidate` recibe:

- país fiscal;
- identificador tal como lo escribió el usuario.

Devuelve `NormalizedBusinessTaxIdentifier` con:

- país ISO alpha-2 en mayúsculas;
- valor canónico;
- esquema reconocido;
- indicador de formato local validado;
- indicador de carácter de control validado.

El contrato nunca incluye el identificador en excepciones. El registro traduce cualquier rechazo a
`REGISTRATION_INVALID`, sin enviar reglas internas ni datos fiscales a la respuesta.

## Canonicalización común

El flujo:

1. recorta extremos;
2. aplica Unicode NFKC;
3. convierte a mayúsculas con `Locale.ROOT`;
4. elimina espacios Unicode, guion, punto y barra como separadores de presentación;
5. rechaza cualquier carácter restante que no sea letra o dígito ASCII;
6. exige entre 2 y 64 caracteres canónicos.

No se transliteran alfabetos ni se elimina puntuación desconocida. Una transformación agresiva
podría hacer colisionar dos identificadores distintos o permitir caracteres visualmente engañosos.

Para países sin estrategia registrada se devuelve el esquema `GENERIC` con
`formatValidated=false` y `controlCharacterValidated=false`. La identidad puede persistirse en
`unverified`, pero ningún consumidor puede interpretarla como fiscalmente válida.

## Estrategia española

España usa un valor nacional de nueve caracteres. Se acepta el prefijo NIF-IVA `ES` cuando acompaña
a ese valor, pero se elimina de la forma canónica: `taxCountry=ES` ya participa en la clave única.
Por tanto, `ES/B-12345674`, `b.1234567-4` y `B12345674` representan la misma identidad.

Esquemas implementados:

- DNI/NIF de persona física: ocho dígitos y letra calculada por módulo 23;
- NIE: prefijo `X`, `Y` o `Z`, siete dígitos y la misma tabla de control tras sustituir el prefijo
  por `0`, `1` o `2`;
- NIF especiales `K`, `L` o `M`: siete dígitos y letra de control por módulo 23;
- NIF de persona jurídica o entidad: una clave de forma jurídica admitida, siete dígitos y carácter
  de control calculado mediante suma alterna.

Para entidades:

- `A`, `B`, `E` y `H` exigen control numérico;
- `N`, `P`, `Q`, `R`, `S` y `W` exigen control alfabético;
- las demás claves vigentes admitidas aceptan la representación numérica o alfabética calculada.

La composición normativa de nueve caracteres y las claves de entidad se basan en la
[Orden EHA/451/2008 consolidada](https://www.boe.es/buscar/act.php?id=BOE-A-2008-3580) y en la
[guía censal de composición del NIF de la AEAT](https://sede.agenciatributaria.gob.es/Sede/ayuda/manuales-videos-folletos/manuales-practicos/guia-practica-cumplimentacion-modelo-censal-036/anexos/anexo-01-solicitud-nif-documentacion-aportar/informacion-sobre-numero-identificacion-fiscal/composicion-nif/personas-juridicas-entidades.html).
La aceptación del prefijo se limita a la representación; la inscripción efectiva como NIF-IVA debe
comprobarse posteriormente mediante
[ROI/VIES](https://sede.agenciatributaria.gob.es/Sede/iva/iva-operaciones-comercio-exterior/identificacion-realizar-operaciones-otros-empresarios-ue.html).

## Unicidad y persistencia

`VenueRegistrationServiceImpl` usa exclusivamente el país y valor devueltos por el servicio para:

1. consultar `BusinessAccountDao.existsByTaxIdentity`;
2. escribir `"taxCountry"` y `"businessTaxIdentifierNormalized"`;
3. dejar el identificador aportado, recortado pero legible, en `"businessTaxIdentifier"`.

El índice único `"uqBusinessAccountsTaxIdentifier"` sobre país y valor normalizado sigue siendo la
autoridad ante carreras. Los prechecks mejoran la respuesta habitual y PostgreSQL resuelve la
concurrencia con el mismo `REGISTRATION_CONFLICT` genérico.

No se añade migración en esta tarea: el esquema V4 ya contiene columna canónica, longitud,
mayúsculas de país e índice único. La validación de checksum es lógica nacional versionable y no se
duplica en SQL.

## Extensión a nuevos países

Cada país con reglas conocidas implementa `CountryBusinessTaxIdentifierValidator` y declara un
único código ISO. El servicio falla al arrancar si dos estrategias intentan gestionar el mismo país.
Una estrategia:

- no puede hacer red;
- recibe el valor ya compactado;
- devuelve esquema y garantías locales;
- lanza una excepción sin datos sensibles si formato o control fallan.

Los adaptadores remotos futuros deben consumir el valor canónico, pero mantener separadas la
validación matemática, la existencia remota y las transiciones de estado.
