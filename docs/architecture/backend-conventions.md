# Convenciones backend automatizadas

## Objetivo

El backend de Reserly debe mantenerse como monolito modular con reglas homogéneas de persistencia, JPA y contratos REST. Esta guía convierte `RNF-011` en reglas operativas y define qué cubre `npm run backend:conventions:check`.

El validador no sustituye al compilador, Checkstyle, Spotless ni la revisión humana. Su función es bloquear desviaciones estructurales frecuentes antes de que entren migraciones, entidades, DAOs, servicios, controladores, DTOs o conversores incompatibles con el diseño.

## Comando

```bash
npm run backend:conventions:check
```

El comando se ejecuta en `npm run verify` y en el job `Quality` de GitHub Actions.

## Migraciones Flyway

Las tablas físicas deben declararse con identificadores entrecomillados `UpperCamelCase`:

```sql
CREATE TABLE "BusinessAccount" (
  "id" uuid PRIMARY KEY,
  "businessTaxIdentifierNormalized" varchar(64) NOT NULL
);
```

Las columnas declaradas explícitamente deben usar identificadores entrecomillados `lowerCamelCase`. Los nombres conceptuales de tareas escritos en `snake_case` deben traducirse antes de crear la migración.

El validador revisa:

- `CREATE TABLE`;
- `ALTER TABLE`;
- columnas dentro de bloques `CREATE TABLE`.

## Entidades JPA

Las entidades deben:

- terminar en `Entity`;
- declarar `@Table(name = "\"UpperCamelCase\"")`;
- declarar columnas explícitas como `@Column(name = "\"lowerCamelCase\"")`;
- declarar relaciones JPA en getters, no en campos;
- mantener setter correspondiente para cada getter con relación.

Ejemplo:

```java
@Entity
@Table(name = "\"BusinessAccount\"")
public class BusinessAccountEntity {
  private UUID id;
  private UserEntity ownerUser;

  @Column(name = "\"id\"")
  public UUID getId() {
    return id;
  }

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "\"ownerUserId\"")
  public UserEntity getOwnerUser() {
    return ownerUser;
  }

  public void setOwnerUser(UserEntity ownerUser) {
    this.ownerUser = ownerUser;
  }
}
```

## DAOs

Los DAOs deben terminar en `Dao` y actuar como repositorios Spring. Las consultas propias del dominio deben declarar `@Query` para que filtros, locks, joins y ordenación queden visibles en revisión.

```java
public interface BusinessAccountDao extends JpaRepository<BusinessAccountEntity, UUID> {
  @Query("select account from BusinessAccountEntity account where account.id = :id")
  Optional<BusinessAccountEntity> findOwnedAccount(UUID id);
}
```

## Servicios y controladores

Los servicios deben separar interfaz e implementación:

- interfaz: `BusinessVerificationService`;
- implementación: `BusinessVerificationServiceImpl`;
- la implementación lleva `@Service`.

Los controladores siguen el mismo patrón:

- interfaz: `VenueProfileController`;
- implementación: `VenueProfileControllerImpl`;
- la implementación lleva `@RestController`.

Las interfaces documentan contrato, permisos, invariantes de negocio, errores esperados y efectos secundarios. Las implementaciones contienen la lógica.

## DTOs y conversores

Los DTOs REST deben vivir en paquetes `dto` y terminar en uno de estos sufijos:

- `Request`;
- `Response`;
- `Command`;
- `Dto`.

Los conversores explícitos deben vivir en paquetes `converter` y terminar en `Converter`. Los controladores no deben devolver entidades JPA directamente.

## Alcance actual del validador

`scripts/validate-backend-conventions.mjs` revisa:

- nombres de tipos Java principales y archivo;
- entidades JPA, `@Table`, `@Column` y relaciones en getters;
- DAOs y métodos propios sin `@Query`;
- `@Service`/`@RestController` con interfaz separada;
- sufijos de DTOs y conversores por paquete;
- tablas y columnas en migraciones Flyway.

Limitaciones:

- No interpreta Java completo ni SQL completo.
- No comprueba todavía dependencias entre módulos o ciclos.
- No sustituye tests de integración ni revisión de permisos.
- Las reglas podrán endurecerse cuando existan entidades y endpoints reales.
