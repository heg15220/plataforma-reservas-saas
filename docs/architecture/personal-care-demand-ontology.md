# Ontología de demanda de cuidado personal v1

## Alcance y versión

La versión personal-care.v1, efectiva desde 2026-08-13, describe exclusivamente el piloto de
peluquería y estética con cita individual definido en 19.1. No cubre salud, restauración, deporte,
grupos, menores, pricing dinámico ni contacto proactivo.

El artefacto fuente vive en packages/demand-contracts/ontology/personal-care.v1.json. Contiene 44
atributos publicados dentro de la puerta exigida de 30-50:

| Familia       | Cantidad | Alcance                                                 |
| ------------- | -------: | ------------------------------------------------------- |
| Ambiente      |        7 | Percepciones agregadas y rasgos ambientales observables |
| Espacio       |        6 | Configuración física del área de atención               |
| Experiencia   |        7 | Recorrido de servicio y agregados con muestra           |
| Oferta        |       10 | Servicios cosméticos estructurados y su jerarquía       |
| Operación     |        8 | Disponibilidad, reserva y políticas transaccionales     |
| Accesibilidad |        6 | Adaptaciones del local declaradas o verificadas         |

Los atributos de oferta incluyen dos nodos jerárquicos (hairServices y skinCareServices) y sus
especializaciones. Los códigos son estables lowerCamel y no se reutilizan con otra semántica.

## Contrato de cada atributo

Cada definición exige nombre y definición ES/EN, familia y padre opcional de la misma familia, tipo,
fuentes gobernadas, vigencia, usos cerrados, mínimo de evidencias y estado publicado. Los atributos
estables duran hasta retirada; dinámicos, relativos y subjetivos caducan entre 1 y 365 días.

Los agregados subjetivos requieren customerAggregate y al menos cinco evidencias; el catálogo usa
umbrales mayores en calma, atención, consistencia, privacidad y amplitud. Una impresión no es
evidencia positiva. El futuro agregador de 19.15 aplicará pesos, diversidad, acuerdo y recencia.

## Fuentes y límites

Las seis fuentes son declaración del local, catálogo estructurado, datos operativos, agregado de
clientes, auditoría verificada e imagen auxiliar. La imagen solo apoya rasgos visuales explícitos
como estilo, luz o zona de espera; nunca determina higiene, seguridad, privacidad, salud o
accesibilidad. Las adaptaciones de accesibilidad describen el local y no permiten inferir la
discapacidad de una persona.

## Prohibiciones

El catálogo enumera 24 prohibiciones: datos sensibles, salud, demografía inferida, vigilancia,
inferencias no sustentadas y equidad laboral. Incluye etnia, religión, orientación sexual, política,
biometría, condición médica, embarazo, discapacidad inferida, edad/género inferidos, fingerprint,
perfil psicológico, higiene, seguridad y ranking de trabajadores individuales.

Un código prohibido no puede solaparse con el catálogo publicado, convertirse en candidato de
clustering ni usarse como feature, filtro o explicación.

## Validación y evolución

JSON Schema 2020-12 aporta interoperabilidad y forma cerrada. Pydantic añade invariantes cruzados:
unicidad, familias completas, fuentes conocidas, padres válidos, ausencia de ciclos, vigencia por
tipo, umbral subjetivo y separación de prohibiciones. Los tests mutan el catálogo para probar fallo
cerrado.

personal-care.v1 es inmutable en significado. Añadir, fusionar o retirar atributos exige el workflow
humano de 19.13 y una nueva versión si rompe compatibilidad. Esta tarea no crea tablas ni panel admin:
el JSON validado será el seed revisable de la siguiente iteración.
