# Módulo 3 · Persistencia con Spring Data JPA

**Objetivo:** modelar la persistencia de las reservas, escribir consultas de distintos tipos, gestionar el esquema con Flyway, aplicar reglas transaccionales y detectar problemas de rendimiento.

```bash
mvn -pl m3-persistencia test                        # H2 + (si hay Docker) PostgreSQL con Testcontainers
cd m3-persistencia && docker compose up -d
mvn -pl m3-persistencia spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Modelo

```
Sala (nombre único, capacidad, Direccion @Embeddable record) 1 ── * Reserva * ── 1 Usuario (email único)
Reserva: inicio, fin, estado (CONFIRMADA | CANCELADA), notas, @Version
```

## Enunciados

### EJ 3.1 · Entidades y relaciones
1. Crea `Sala`, `Usuario` y `Reserva`. Usa `@ManyToOne(fetch = LAZY)` y define el *enum* `EstadoReserva` con `@Enumerated(STRING)`.
2. Modela `Direccion` como **record** `@Embeddable` (Hibernate 6.2+).
3. Añade `@Version` a `Reserva` para el bloqueo optimista.

### EJ 3.2 · Repositorios y consultas
1. Consultas derivadas: `findByNombre`, `findByCapacidadGreaterThanEqualOrderByCapacidadAsc` y `findByDireccionCiudadIgnoreCase` (propiedad anidada).
2. `@Query` JPQL con parámetros con nombre: `existeSolape(salaId, inicio, fin, estado)`.
3. Proyección **DTO** con *constructor expression* (`OcupacionSala`) y proyección por **interfaz** con una proyección anidada (`ReservaResumen`).

*Test:* `RepositoriosTest`.

### EJ 3.3 · Búsqueda dinámica con Specifications
Implementa `ReservaSpecs` con criterios combinables (sala, ciudad, estado y rango de fechas) y la búsqueda paginada y ordenada con `JpaSpecificationExecutor`. Sólo se deben aplicar los filtros informados (`Specification.allOf`).

### EJ 3.4 · El problema N+1
1. Activa `hibernate.generate_statistics` y cuenta las sentencias que genera `findByEstado(...)` cuando después se accede a `r.getSala()`.
2. Resuélvelo con `@EntityGraph(attributePaths = {"sala", "usuario"})` y comprueba que se ejecuta **una sola** consulta.
3. *Alternativas para comentar:* `JOIN FETCH`, `@BatchSize` y `hibernate.default_batch_fetch_size`, y usar proyecciones.

*Test:* `NMasUnoTest`.

### EJ 3.5 · Esquema con Flyway y transacciones
1. `V1__esquema_inicial.sql` crea las tablas; `V2__notas_indices_y_datos.sql` añade una columna, un índice y datos de ejemplo. Hibernate sólo **valida** (`ddl-auto: validate`).
2. `ReservaService#reservar` aplica las reglas de negocio: fin posterior al inicio, sala y usuario existentes y sin solapes.
3. `reservarVarias` debe ser **todo o nada**: si falla una reserva, no se guarda ninguna.
4. `cancelar` usa *dirty checking*, sin llamar a `save`.
5. Provoca una `ObjectOptimisticLockingFailureException` con dos copias de la misma reserva.

*Test:* `ReservaServiceTest`. Fíjate en que **no** es `@Transactional`: así se observan los commits y los rollbacks reales.

### EJ 3.6 · Pruebas con PostgreSQL real
Ejecuta las migraciones y las consultas contra PostgreSQL con Testcontainers y `@ServiceConnection`. *Test:* `PostgresRepositoryTest`, que se omite si no hay Docker.

## Extra Spring Boot 4
- Hibernate 7 / JPA 3.2: prueba `EntityManagerFactory#runInTransaction`/`callInTransaction` y `@EnumeratedValue` para guardar un código en lugar del nombre del *enum*.
- Starters nuevos: `spring-boot-starter-flyway` para Flyway y `spring-boot-starter-data-jpa-test` para los tests.
- Revisa las deprecaciones de Spring Data 2025.1, por ejemplo `Specification.where(..)`. Este proyecto ya usa `Specification.allOf(..)`.
