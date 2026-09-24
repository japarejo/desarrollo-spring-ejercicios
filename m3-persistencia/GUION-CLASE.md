# Guión de clase · Módulo 3 · Persistencia con Spring Data JPA

**Duración:** 3 h 45 min netas, **repartidas en dos sesiones** · Enunciados en [README.md](README.md)

- **Final de la sesión 2 (45 min):** pasos 1 y 2 — el modelo y las entidades.
- **Sesión 3 (3 h):** pasos 3 a 9 — consultas, N+1, transacciones y Flyway.

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer. Comandos **desde la raíz del repositorio**; en Windows, `mvnw.cmd`.

Es el módulo más largo y el que más dudas genera. Dos avisos:

- **Provoca los errores en clase.** `LazyInitializationException` y el N+1 se aprenden viéndolos, no leyéndolos.
- **No abrevies el paso 6.** Es el contenido con más retorno de todo el curso.

---

## Paso 0 · Antes de entrar en el aula (10 min)

```bash
./mvnw -pl m3-persistencia test         # 4 clases; PostgresRepositoryTest se omite si no hay Docker
docker pull postgres:17-alpine          # para el paso 8
git status --short
```

Pestañas del IDE, en orden de uso:

1. [`src/main/java/com/atech/curso/m3/dominio/Reserva.java`](src/main/java/com/atech/curso/m3/dominio/Reserva.java)
2. [`src/main/java/com/atech/curso/m3/repositorio/ReservaRepository.java`](src/main/java/com/atech/curso/m3/repositorio/ReservaRepository.java)
3. [`src/main/java/com/atech/curso/m3/repositorio/ReservaSpecs.java`](src/main/java/com/atech/curso/m3/repositorio/ReservaSpecs.java)
4. [`src/test/java/com/atech/curso/m3/NMasUnoTest.java`](src/test/java/com/atech/curso/m3/NMasUnoTest.java)
5. [`src/main/java/com/atech/curso/m3/servicio/ReservaService.java`](src/main/java/com/atech/curso/m3/servicio/ReservaService.java)
6. [`src/main/resources/db/migration/`](src/main/resources/db/migration/)

---

## Cronograma

| Paso | Contenido | Min | Sesión |
|---|---|---|---|
| 1 | El modelo en la pizarra + gancho | 10 | 2 |
| 2 | Entidades y relaciones (EJ 3.1) | 35 | 2 |
| 3 | Repaso y concepto: las cuatro formas de consultar | 10 | 3 |
| 4 | Ejercicio EJ 3.2 | 35 | 3 |
| 5 | Specifications (EJ 3.3) | 30 | 3 |
| 6 | 🔴 El problema N+1 (EJ 3.4) | 35 | 3 |
| 7 | Flyway y transacciones (EJ 3.5) | 45 | 3 |
| 8 | PostgreSQL real con Testcontainers (EJ 3.6) | 15 | 3 |
| 9 | Cierre del módulo | 10 | 3 |

---

## Paso 1 · El modelo en la pizarra + gancho · 10 min

1. **Dibuja esto y déjalo toda la sesión** (no lo proyectes: dibújalo, se recuerda mejor):

   ```
   Sala 1 ──── * Reserva * ──── 1 Usuario
    │                 │
    │                 ├─ inicio, fin
    │                 ├─ estado: CONFIRMADA | CANCELADA
    │                 └─ @Version   (bloqueo optimista)
    └─ Direccion (@Embeddable record: calle, ciudad, codigoPostal)
   ```

2. **❓ Gancho:** «En el módulo 2 guardábamos reservas con `ddl-auto: create-drop` y H2 en memoria. ¿Qué
   pasa con los datos al reiniciar?» → Se pierden. «¿Y quién crea las tablas en producción? ¿La aplicación,
   al arrancar, adivinando?» Deja el silencio incómodo un par de segundos.

3. **🗣️ Anuncia el módulo:** «Hoy: entidades de verdad, consultas que sabemos qué SQL generan,
   transacciones que se deshacen cuando toca, y un esquema versionado como el código. Y una hora antes
   del final veréis el error de rendimiento más caro y más común de JPA.»

---

## Paso 2 · Entidades y relaciones (EJ 3.1) · 35 min

1. **✏️ Proyecta [`Reserva.java`](src/main/java/com/atech/curso/m3/dominio/Reserva.java)** y recórrelo
   señalando **solo estas cinco cosas** (no leas los *getters*):

   | Línea | Qué señalar y qué decir |
   |---|---|
   | 30 y 34 | `@ManyToOne(fetch = FetchType.LAZY)`. «JPA dice que `@ManyToOne` es `EAGER` por defecto. **Es una mala decisión heredada**: convierte cada consulta en un `JOIN` que casi nunca necesitas. Ponedlo siempre en `LAZY`.» |
   | 45-47 | `@Enumerated(EnumType.STRING)`. **❓ «¿Qué pasa si alguien añade un valor en medio del enum y estaba en `ORDINAL`?»** → Se corrompen los datos históricos, en silencio, y no hay vuelta atrás. |
   | 48 | El valor por defecto `CONFIRMADA` va en el campo, no en el SQL. |
   | 54-55 | `@Version`. «Un número que sube en cada actualización. Lo usaremos en el paso 7.» |
   | 57-58 | El constructor `protected` sin argumentos: lo exige JPA, no vuestro código. |

2. **✏️ Proyecta [`Direccion.java`](src/main/java/com/atech/curso/m3/dominio/Direccion.java)** — 8 líneas:

   ```java
   @Embeddable
   public record Direccion(String calle, String ciudad, String codigoPostal) { }
   ```

   **🗣️ Di:** «Un `record` como tipo embebido, desde Hibernate 6.2. Las tres columnas viven en la tabla
   `sala`, pero en el código son un objeto inmutable. Si vuestro modelo tiene cinco campos que siempre van
   juntos, tenéis un `@Embeddable` esperando.»

3. **Ejercicio (25 min):** crear `Sala`, `Usuario`, `Reserva`, el *enum* `EstadoReserva` y `Direccion`.

   **⌨️ Criterio de aceptación:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=RepositoriosTest#elRecordEmbebidoSeCargaCorrectamente
   ```

4. **🗣️ Cierra la sesión 2 aquí** y anuncia: «Mañana empezamos consultando estas entidades de cuatro formas
   distintas y decidiendo cuál toca en cada caso.»

---

## Paso 3 · Repaso y concepto: las cuatro formas de consultar · 10 min

1. **🗣️ Repaso de 2 minutos** del dibujo de la pizarra: entidades, relaciones `LAZY`, `@Version`.

2. **🗣️ Las cuatro familias**, escritas en la pizarra en una columna, con su criterio de elección:

   | Forma | Cuándo |
   |---|---|
   | **Derivada** (`findByNombre`) | Filtros simples. Si el nombre del método no te cabe en una línea, no es esto. |
   | **JPQL** con `@Query` | Cuando la consulta tiene lógica. Se lee, se revisa y **se valida al arrancar**. |
   | **Proyección** (interfaz o DTO) | Cuando solo quieres leer unos campos. No cargas entidades, no hay estado sucio que vigilar. |
   | **Specification** | Cuando los filtros son dinámicos y opcionales. Paso 5. |

3. **🗣️ El aviso que hay que dar aquí:** «Spring Data os ahorra escribir el `DAO`. **No os ahorra saber qué
   SQL se ejecuta.** Durante todo el módulo vamos a tener el log de SQL a la vista; en
   [`application.yml`](src/main/resources/application.yml) está `logging.level.org.hibernate.SQL: debug` y
   `format_sql: true`.»

---

## Paso 4 · Ejercicio EJ 3.2 · 35 min

1. **✏️ Proyecta [`ReservaRepository.java`](src/main/java/com/atech/curso/m3/repositorio/ReservaRepository.java)**
   y comenta cuatro métodos, 30 segundos cada uno:

   - **Línea 18** `findByUsuarioEmailOrderByInicioAsc`: navega por la relación (`usuario.email`) **y**
     devuelve una proyección por interfaz.
   - **Líneas 29-35** `existeSolape`: JPQL con parámetros con nombre y bloque de texto.
     **🗣️ Di:** «Los parámetros con nombre son legibles y no se descolocan al añadir uno. Los posicionales
     (`?1`) son una bomba de relojería en cuanto la consulta crece.»
   - **Líneas 38-43** `ocupacionPorSala`: *constructor expression*, `select new com.atech...OcupacionSala(...)`.
     **🗣️ Aviso:** «Hay que poner el nombre **completo** de la clase. Es feo y es así.»
   - **[`ReservaResumen.java`](src/main/java/com/atech/curso/m3/repositorio/ReservaResumen.java)**: proyección
     por interfaz con una proyección **anidada** (`SalaNombre`). No hay implementación: la genera Spring Data.

2. **❓ Pregunta:** «Proyección por interfaz o DTO, ¿cuál?» → La interfaz es más cómoda y permite anidar; el
   DTO con *constructor expression* deja explícito el SQL y es lo único que funciona con `group by`. Las dos
   valen; lo que no vale es cargar la entidad entera para leer dos campos.

3. **Ejercicio (25 min). ⌨️ Criterio:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=RepositoriosTest
   ```

4. **Mientras trabajan**, ten a mano estas respuestas:

   | Pregunta que te harán | Respuesta |
   |---|---|
   | «¿Cómo sé si el nombre del método derivado es válido?» | Si está mal, **la aplicación no arranca** y el error dice la propiedad que no existe. Es una validación temprana, no un fastidio. |
   | «¿Puedo devolver `Optional`?» | Sí, y es lo correcto para el 0-o-1. |
   | «¿Y si necesito SQL nativo?» | `@Query(nativeQuery = true)`. Funciona, pero pierdes portabilidad y validación al arrancar. |

---

## Paso 5 · Specifications (EJ 3.3) · 30 min

1. **🗣️ Plantea el problema real (3 min):** «Un buscador con cinco filtros, todos opcionales. ¿Cuántos
   métodos `findBy...` hacen falta?» → 2⁵ = 32 combinaciones. Que hagan la cuenta ellos.

2. **🗣️ La alternativa que veréis en las empresas:** concatenar cadenas para montar el JPQL. «Funciona,
   es imposible de leer a los seis meses, y en cuanto alguien meta un parámetro sin `setParameter` tenéis
   una inyección.»

3. **✏️ Proyecta [`ReservaSpecs.java`](src/main/java/com/atech/curso/m3/repositorio/ReservaSpecs.java)**:
   - **Líneas 21-40:** un criterio, un método, cada uno independiente y probable por separado.
   - **Línea 26:** `enCiudad` navega a `sala.direccion.ciudad` — atraviesa el `@Embeddable`.
   - **Líneas 43-61:** `de(Filtro)` solo añade lo informado y termina con `Specification.allOf(specs)`.
     **🗣️ Nota:** «`Specification.where(...)` está deprecado en Spring Data 2025.1; este proyecto ya usa
     `allOf`.»

4. **Ejercicio (20 min). ⌨️ Criterio:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest='RepositoriosTest#busquedaDinamicaPaginadaConSpecifications+sinFiltrosDevuelveTodo'
   ```

   **✏️ Señala el segundo test:** sin ningún filtro, `allOf` de una lista vacía devuelve **todo**, no nada.
   Es el caso límite que se olvida siempre.

5. **🗣️ Cierra con el criterio de uso:** «Si los filtros son fijos, consulta derivada o JPQL. Specifications
   solo cuando son de verdad dinámicos: son más código y más difíciles de leer.»

---

## Paso 6 · 🔴 El problema N+1 (EJ 3.4) · 35 min

**El bloque estrella del módulo. Sigue estos siete puntos en orden.**

1. **✏️ Proyecta [`NMasUnoTest.java`](src/test/java/com/atech/curso/m3/NMasUnoTest.java)** y enseña **solo**
   la cabecera y el primer test, sin las aserciones (tápalas o haz *scroll* justo antes):

   ```java
   @DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
   ...
   List<Reserva> confirmadas = reservas.findByEstado(EstadoReserva.CONFIRMADA);
   confirmadas.forEach(r -> r.getSala().getNombre());
   ```

2. **❓ Pregunta y haz votar a mano alzada:** «Hay 4 reservas confirmadas en 3 salas distintas.
   ¿Cuántas consultas SQL se lanzan: 1, 2 o más?» **La mayoría dirá 1.**

3. **⌨️ Ejecuta:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=NMasUnoTest#consultaDerivadaProvocaNMasUno
   ```

4. **✏️ Busca en el log los bloques `org.hibernate.SQL`** y proyéctalos. Verás, uno detrás de otro:

   ```
   select r1_0.id, r1_0.estado, r1_0.fin, ... from reserva r1_0 where r1_0.estado=?
   select s1_0.id, s1_0.capacidad, s1_0.calle, ... from sala s1_0 where s1_0.id=?
   select s1_0.id, s1_0.capacidad, s1_0.calle, ... from sala s1_0 where s1_0.id=?
   select s1_0.id, s1_0.capacidad, s1_0.calle, ... from sala s1_0 where s1_0.id=?
   ```

   **✏️ Y ahora sí, enseña la aserción** ([línea 49](src/test/java/com/atech/curso/m3/NMasUnoTest.java#L49)):

   ```java
   assertThat(estadisticas.getPrepareStatementCount()).isEqualTo(4);   // 1 + 3 salas
   ```

   **🗣️ Di:** «Uno más ene. Con 4 filas no lo nota nadie. Con 30 000 en producción, son 30 000 idas y
   vueltas a la base de datos y una incidencia de rendimiento que nadie sabe explicar.»

5. **⌨️ Ahora la solución:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=NMasUnoTest#entityGraphLoResuelveConUnaSolaConsulta
   ```

   **✏️ Enseña las dos líneas del repositorio**
   ([líneas 24-26](src/main/java/com/atech/curso/m3/repositorio/ReservaRepository.java#L24-L26)):

   ```java
   @EntityGraph(attributePaths = { "sala", "usuario" })
   @Query("select r from Reserva r where r.estado = :estado")
   ```

   **⌨️ Comprobación visual rotunda** (cuenta las consultas sueltas a `sala`; devuelve **0**):

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=NMasUnoTest#entityGraphLoResuelveConUnaSolaConsulta | grep -c "s1_0.id=?"
   ```

6. **🗣️ Las cuatro alternativas y cuándo usar cada una** (escríbelas en la pizarra):

   | Técnica | Cuándo |
   |---|---|
   | `@EntityGraph` | Declarativo y reutilizable. **La opción por defecto.** |
   | `JOIN FETCH` en JPQL | Para una consulta concreta. Ojo al paginar con colecciones: Hibernate pagina en memoria. |
   | `@BatchSize` / `hibernate.default_batch_fetch_size` | Red de seguridad global: convierte 1+N en 1+N/tamaño. Actívalo en todos los proyectos. |
   | Proyecciones | Si solo vas a leer, no cargues entidades. |

7. **🗣️ Cierre del bloque:** «Dos hábitos que os podéis llevar hoy mismo: tener el log de SQL encendido en
   desarrollo, y poner `default_batch_fetch_size` en vuestro `application.yml` esta misma tarde.»

---

## Paso 7 · Flyway y transacciones (EJ 3.5) · 45 min

### 7a · El esquema como código (15 min)

1. **✏️ Abre la carpeta [`src/main/resources/db/migration/`](src/main/resources/db/migration/)** y enseña
   los dos ficheros: `V1__esquema_inicial.sql` y `V2__notas_indices_y_datos.sql`.
   **🗣️ La convención:** `V` + versión + `__` (dos guiones bajos) + descripción.

2. **✏️ Enseña el V2**: una columna nueva, un índice y datos de ejemplo. «Así crece un esquema en
   producción: **añadiendo**, nunca editando lo ya aplicado.»

3. **✏️ Y ahora [`application.yml` línea 8](src/main/resources/application.yml#L8):** `ddl-auto: validate`.
   **🗣️ Di:** «Hibernate ya no crea nada: **comprueba** que el esquema que ha hecho Flyway coincide con las
   entidades. Si no coincide, la aplicación no arranca.»

4. **🔴 Rotura (2 min):** en [`Reserva.java` línea 52](src/main/java/com/atech/curso/m3/dominio/Reserva.java#L52),
   cambia el nombre del campo:

   ```java
   private String observaciones;   // antes: notas
   ```

   **⌨️ Ejecuta** (fallará al validar el esquema):

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=RepositoriosTest#consultasDerivadas
   ```

   **Salida esperada** (proyéctala: el mensaje es exacto y útil):

   ```
   Failed to initialize JPA EntityManagerFactory: ...
   org.hibernate.tool.schema.spi.SchemaManagementException:
       Schema-validation: missing column [observaciones] in table [reserva]
   ```

   **🗣️ Di:** «Ha fallado **al arrancar**, no en producción a las tres de la mañana. Comparadlo con
   `ddl-auto: update`, que habría añadido la columna en silencio y os habría dejado dos columnas y los datos
   en la vieja.»

   **↩️ Deshaz:**

   ```bash
   git checkout -- m3-persistencia/src/main/java/com/atech/curso/m3/dominio/Reserva.java
   ```

5. **🗣️ La regla de oro, dicha en voz alta y despacio:** «Una migración ya aplicada **no se toca jamás**.
   Si está mal, se añade otra que lo arregle. Quien edite una `V2` que ya está en producción, romperá el
   arranque de todos los entornos con un error de *checksum*.»

### 7b · Transacciones (30 min)

1. **✏️ Proyecta [`ReservaService.java`](src/main/java/com/atech/curso/m3/servicio/ReservaService.java)** y
   señala tres cosas:
   - **Línea 26:** `@Transactional(readOnly = true)` en la clase; los métodos que escriben lo sobrescriben
     (líneas 42, 58, 63). **🗣️ «`readOnly` no es decorativo: le ahorra a Hibernate el control de cambios.»**
   - **Líneas 44-54:** las reglas de negocio, en el servicio, no en el controlador ni en la entidad.
   - **Líneas 63-69 `cancelar`:** modifica el objeto y **no llama a `save`**.
     **❓ «¿Por qué se guarda igualmente?»** → *Dirty checking*: dentro de la transacción, la entidad está
     gestionada y Hibernate compara y actualiza al hacer *commit*.

2. **⌨️ Ejecuta la clase entera y proyecta el resultado:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=ReservaServiceTest
   ```

3. **✏️ Enseña el comentario de la cabecera del test**
   ([líneas 21-24](src/test/java/com/atech/curso/m3/ReservaServiceTest.java#L21-L24)):
   **el test no es `@Transactional` a propósito.**
   **🗣️ Di:** «Esto es importante y casi nadie lo sabe: si anotáis el test con `@Transactional`, al final se
   hace *rollback* de todo y **nunca veis si vuestro código hacía bien el *commit***. Un test transaccional
   puede pasar con código que en producción no guarda nada.»

4. **🔴 Rotura: quita el todo o nada.**
   **✏️ En [`ReservaService.java` línea 58](src/main/java/com/atech/curso/m3/servicio/ReservaService.java#L58)**,
   comenta el `@Transactional` de `reservarVarias`.

   **⌨️ Ejecuta:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=ReservaServiceTest#reservarVariasEsTodoONada
   ```

   El test falla: la primera reserva **se ha quedado guardada** aunque la segunda reventó.

   **🗣️ Di:** «Esto es una factura a medias, un pedido sin líneas, media transferencia. Y fijaos en que el
   método sigue funcionando: solo está mal cuando algo falla, que es justo cuando nadie mira.»

   **↩️ Deshaz:**

   ```bash
   git checkout -- m3-persistencia/src/main/java/com/atech/curso/m3/servicio/ReservaService.java
   ```

5. **❓ Pregunta trampa (vuelve a la pizarra del módulo 1):** «`reservarVarias` llama a `this.reservar(...)`,
   que también es `@Transactional`. ¿Se abre una transacción nueva?»
   → **No.** Es una auto-invocación: no pasa por el proxy. Aquí da igual porque queremos una sola
   transacción, pero si alguien pusiera `REQUIRES_NEW` en `reservar` esperando otra cosa, **no funcionaría**.
   Mismo mecanismo que el EJ 1.5.

6. **⌨️ Bloqueo optimista (5 min):**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=ReservaServiceTest#bloqueoOptimistaDetectaActualizacionesConcurrentes
   ```

   **✏️ Proyecta el test** ([líneas 72-86](src/test/java/com/atech/curso/m3/ReservaServiceTest.java#L72-L86)):
   dos copias leídas a la vez, la segunda al guardar lanza `ObjectOptimisticLockingFailureException`.

   **❓ Pregunta:** «¿Y qué hacéis con esa excepción de cara al usuario?» → Releer y reintentar, o devolver un
   `409 Conflict` y que decida la persona. Lo que no vale es tragársela: eso es perder el cambio de alguien
   sin decírselo.

---

## Paso 8 · PostgreSQL real con Testcontainers (EJ 3.6) · 15 min

1. **⌨️ Con Docker arrancado:**

   ```bash
   ./mvnw -pl m3-persistencia test -Dtest=PostgresRepositoryTest
   ```

2. **✏️ Proyecta la cabecera del test**
   ([líneas 22-28](src/test/java/com/atech/curso/m3/PostgresRepositoryTest.java#L22-L28)):

   ```java
   @DataJpaTest
   @AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
   @Testcontainers(disabledWithoutDocker = true)
   class PostgresRepositoryTest {
       @Container @ServiceConnection
       static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
   ```

   **🗣️ Tres cosas, una por anotación:**
   - `@ServiceConnection`: **no hay que configurar URL, usuario ni contraseña.** Boot las saca del contenedor.
   - `Replace.NONE`: si no, Boot sustituiría el `DataSource` por H2 y estaríamos probando otra cosa.
   - `disabledWithoutDocker = true`: sin Docker el test **se omite**, no falla. Por eso el proyecto entero
     compila en cualquier portátil.

3. **❓ Pregunta:** «¿Por qué no basta con H2?» → Porque H2 no es PostgreSQL: tipos, funciones de fecha,
   *secuencias*, bloqueos y el comportamiento de `ON CONFLICT` son distintos. Las migraciones de Flyway con
   SQL específico **solo se prueban de verdad contra el motor real**.

4. **🗣️ Recomendación práctica:** «Unitarios y de repositorio rápidos con H2; los de esquema y los de
   consultas críticas, con Testcontainers. Y en el *pipeline*, todos.»

---

## Paso 9 · Cierre del módulo · 10 min

1. **⌨️ Que todos ejecuten:**

   ```bash
   ./mvnw -pl m3-persistencia test
   ```

   Mínimo exigible: `RepositoriosTest`, `NMasUnoTest` y `ReservaServiceTest#reservarVariasEsTodoONada`.

2. **↩️ Limpieza:**

   ```bash
   docker compose -f m3-persistencia/compose.yaml down
   git status --short
   ```

3. **🗣️ Las tres frases del módulo:**
   - Spring Data te ahorra el `DAO`, no el saber SQL: ten el log a la vista.
   - El esquema es código: se versiona, se revisa y no se toca a mano.
   - Y una transacción solo demuestra que funciona cuando algo falla dentro de ella.

4. **Enlaza con el módulo 4:** «Ya tenemos dominio, consultas y transacciones. Falta que alguien pueda usar
   esto desde fuera: mañana, la web y la API.»

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 8 (Testcontainers) | 15 min | Ejecútalo tú y enseña la cabecera. Es demo, no ejercicio. |
| Paso 5, ejercicio | 20 min | Proyecta `ReservaSpecs` ya hecho y ejecuta el test. |
| Paso 7a, puntos 1-3 | 10 min | Da las migraciones por vistas y quédate con `validate` y la regla de oro. |

**No recortes el paso 6 ni el 7b:** N+1 y transacciones son la razón de ser del módulo.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa | Qué decir |
|---|---|---|
| `LazyInitializationException` | Se navega a la relación fuera de la transacción | «Provocadlo aposta una vez: es el error que más vais a ver.» Se arregla con `@EntityGraph`, proyección o haciendo el trabajo dentro del servicio |
| `Schema-validation: missing column` | La entidad y la migración no coinciden | Es `ddl-auto: validate` haciendo su trabajo |
| Flyway: `checksum mismatch` | Han editado una migración ya aplicada | Regla de oro. Para desatascar en clase: `docker compose down -v` |
| El `record` `@Embeddable` no carga | Hibernate anterior a 6.2 | Comprobar la versión de Boot |
| El test pasa pero en producción no guarda | El test es `@Transactional` y hace *rollback* | Enseñar la cabecera de `ReservaServiceTest` |
| `ObjectOptimisticLockingFailureException` inesperada | Dos hilos con la misma entidad | Es una buena noticia: el bloqueo optimista funciona |
