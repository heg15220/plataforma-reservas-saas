# Identidad seudónima del motor de demanda

## Alcance

El dominio `demand.identity` reconoce actividad consentida sin copiar el email operativo al dataset
analítico. `CustomerIdentities` conserva una unión mediante HMAC-SHA-256 versionado;
`AnonymousIdentities`, un UUID aleatorio de primera parte; e `IdentityLinks`, la evidencia revocable
que vincula ambas identidades para una finalidad concreta. No implementa cookies, endpoints, UI ni
jobs de borrado y no altera el flujo de reserva.

## HMAC y rotación

El productor normaliza el email según el contrato operativo y calcula
`HMAC-SHA-256(normalizedEmail, secretKey)`. PostgreSQL recibe únicamente 64 caracteres hexadecimales
en minúsculas y `keyVersion`; nunca el email ni el secreto. Un SHA-256 simple no es válido.

La unicidad `(keyVersion, emailHmac)` permite coexistencia durante rotación. Esta requiere habilitar
la clave nueva en el gestor de secretos, producir otra versión, relacionar versiones solo mediante
un proceso controlado autorizado, revocar vínculos antiguos, reconstruir derivados consentidos y
retirar la clave previa tras su ventana. Las claves nunca se registran ni persisten.

## Consentimiento, finalidad y vigencia

Una identidad puede existir sin personalización para atender revocación, retención o deduplicación
permitida. Solo es personalizable con versión/fecha de consentimiento, sin revocación y dentro de
vigencia y retención. Cada vínculo registra motivo, finalidad, versión, aceptación, vinculación,
revocación y retención. La allowlist separa analítica, personalización, experimentación y activación
comercial. Revocar no borra reservas ni bloquea la operativa.

## Minimización, DAOs y concurrencia

El UUID anónimo debe proceder de un CSPRNG y nunca de IP, user-agent, canvas, hardware, publicidad o
fingerprinting. Constraints impiden HMAC malformado, consentimiento parcial, tiempos incoherentes,
valores desconocidos y vínculos activos duplicados. FKs `RESTRICT` fuerzan borrado propagado
explícito.

Los DAOs exponen HMAC versionado, identidad personalizable, vínculo activo, revocación atómica y
lotes de retención. `touchActive` no amplía expiración/retención ni reactiva revocados. La creación
de derivados debe revalidar consentimiento en su transacción. Logs: solo UUID técnico, finalidad,
versión y resultado; nunca HMAC, email, secreto o señales del dispositivo.
