# Guión de clase · Módulo 2 · Spring Boot a fondo

**Duración:** 2 h 45 min netas · **Sesión 2** del curso · Enunciados en [README.md](README.md)

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer.
> Todos los comandos se lanzan **desde la raíz del repositorio**. En Windows, `mvnw.cmd`.

---

## Paso 0 · Antes de entrar en el aula (10 min)

```bash
./mvnw -pl m2-boot test                 # 3 clases de test en verde
docker pull postgres:17-alpine          # para el paso 4 (perfil prod)
git status --short                      # repositorio limpio
```

Ten preparado:

- Una **terminal con el proyecto** y otra **libre para `curl`**, las dos visibles a la vez.
- Estas pestañas en el IDE:
  1. [`src/main/resources/application.yml`](src/main/resources/application.yml)
  2. [`src/main/java/com/atech/curso/m2/config/ReservasProperties.java`](src/main/java/com/atech/curso/m2/config/ReservasProperties.java)
  3. [`src/main/resources/application-dev.yml`](src/main/resources/application-dev.yml) y [`application-prod.yml`](src/main/resources/application-prod.yml)
  4. [`src/main/java/com/atech/curso/m2/operacion/SalasHealthIndicator.java`](src/main/java/com/atech/curso/m2/operacion/SalasHealthIndicator.java)
  5. [`src/main/java/com/atech/curso/m2/autoconfig/RelojAutoConfiguration.java`](src/main/java/com/atech/curso/m2/autoconfig/RelojAutoConfiguration.java)

**Si vienes del módulo 1**, empieza recordando la pizarra del proxy en 30 segundos: hoy no se toca, pero
vuelve a aparecer en el paso 6 sin avisar.

---

## Cronograma

| Paso | Contenido | Min | Acumulado |
|---|---|---|---|
| 1 | 🔴 Gancho: la aplicación que no arranca | 10 | 0:10 |
| 2 | Concepto: configuración tipada y validada | 15 | 0:25 |
| 3 | Ejercicio EJ 2.1 | 30 | 0:55 |
| 4 | Perfiles y logging estructurado (EJ 2.2 y 2.3) | 25 | 1:20 |
| 5 | Actuator y métricas (EJ 2.4) | 35 | 1:55 |
| 6 | Hilos virtuales e imagen OCI (EJ 2.5) | 15 | 2:10 |
| 7 | Autoconfiguración propia (EJ 2.6) | 25 | 2:35 |
| 8 | Cierre del módulo | 10 | 2:45 |

---

## Paso 1 · 🔴 Gancho: la aplicación que no arranca · 10 min

1. **⌨️ Ejecuta esto sin decir lo que va a pasar:**

   ```bash
   ./mvnw -q -pl m2-boot spring-boot:run -Dspring-boot.run.arguments=--atech.reservas.hora-apertura=25
   ```

2. **Salida esperada** (proyéctala entera y déjala en pantalla mientras hablas):

   ```
   ***************************
   APPLICATION FAILED TO START
   ***************************

   Description:

   Binding to target com.atech.curso.m2.config.ReservasProperties failed:

       Property: atech.reservas.horaApertura
       Value: "25"
       Origin: "atech.reservas.hora-apertura" from property source "commandLineArgs"
       Reason: debe ser menor que o igual a 23

   Action:

   Update your application's configuration
   ```

3. **🗣️ Di:** «La aplicación se ha negado a arrancar. Y fijaos en lo que dice: qué propiedad, qué valor,
   **de dónde venía ese valor** y por qué no vale. Esto no lo he programado yo: lo da Boot cuando declaras
   la configuración con tipos y validación.»

4. **❓ Pregunta:** «¿Qué habría pasado en vuestra aplicación con una hora de apertura igual a 25?»
   → Lo normal: arranca tan contenta y falla dentro de unas horas, en la primera reserva nocturna, con un
   error que no se parece en nada a la causa.

5. **🗣️ Cierra el gancho:** «El objetivo de la primera hora de hoy es que **la configuración inválida mate
   el arranque**, no la producción.»

---

## Paso 2 · Concepto: configuración tipada y validada · 15 min

1. **✏️ Proyecta [`ReservasProperties.java`](src/main/java/com/atech/curso/m2/config/ReservasProperties.java)**
   y recórrelo de arriba abajo, señalando cada elemento:

   | Línea | Qué señalar |
   |---|---|
   | 27-28 | `@Validated` + `@ConfigurationProperties("atech.reservas")`: el prefijo y la orden de validar. |
   | 29 | Es un **`record`**: inmutable, sin *setters*, enlazado por constructor. |
   | 30 | `@NotEmpty List<String> salas`: obligatoria. |
   | 31 | `@DefaultValue("4h") Duration duracionMaxima`: **tipo rico**, no `String` ni `int`. |
   | 32-33 | `@Min`/`@Max` sobre los enteros. |
   | 34 | `@DefaultValue @Valid Notificaciones`: configuración **anidada** que también se valida. |
   | 41-43 | `salaPermitida(...)`: las propiedades pueden tener lógica; no son un saco de campos. |

2. **🗣️ Insiste en `Duration`:** «`duracion-maxima: 4h` se escribe así en el YAML y llega como `Duration`.
   También hay `DataSize` (`10MB`) y `Period`. Cada vez que veáis un `int minutos` en una configuración,
   hay un tipo mejor esperando.»

3. **✏️ Enseña [`M2Application.java` línea 8](src/main/java/com/atech/curso/m2/M2Application.java#L8):**
   `@ConfigurationPropertiesScan`. **🗣️ Di:** «Sin esto —o sin `@EnableConfigurationProperties`— el record
   no se registra y os pasaréis veinte minutos buscando por qué está a `null`. Es el fallo número uno del
   módulo.»

4. **🗣️ El procesador de configuración:** el `pom.xml` incluye `spring-boot-configuration-processor`.
   **✏️ Demuéstralo:** abre [`application.yml`](src/main/resources/application.yml), ponte en la línea de
   `atech.reservas.` y pulsa `Ctrl+Espacio`: el IDE autocompleta **tus** propiedades y muestra el javadoc del
   `record`. Es la razón práctica de poner comentarios en los parámetros.

---

## Paso 3 · Ejercicio EJ 2.1 · 30 min

1. **🗣️ Enunciado y criterio de aceptación:**

   ```bash
   ./mvnw -pl m2-boot test -Dtest=ReservasPropertiesTest
   ```

   Cuatro tests: enlace y valores por defecto, fallo sin salas, fallo con hora fuera de rango y fallo en la
   configuración anidada.

2. **✏️ Antes de soltarlos, proyecta el test**
   ([`ReservasPropertiesTest.java` líneas 22-35](src/test/java/com/atech/curso/m2/ReservasPropertiesTest.java#L22-L35))
   y explica `ApplicationContextRunner`:

   ```java
   runner.withPropertyValues("atech.reservas.salas=Turing,Hopper", "atech.reservas.duracion-maxima=90m")
         .run(ctx -> { ... });
   ```

   **🗣️ Di:** «Esto levanta un contexto mínimo, con las propiedades que tú le das, en milisegundos. No hay
   `@SpringBootTest`, no hay base de datos, no hay servidor. Para probar configuración es **la** herramienta,
   y casi nadie la conoce.»

3. **Fíjate en `hasFailed()`** (línea 39): «probar que **no** arranca es tan importante como probar que sí.»

4. **Mientras trabajan, los tres atascos habituales:**

   | Síntoma | Solución |
   |---|---|
   | Las propiedades llegan a `null` | Falta `@ConfigurationPropertiesScan` o `@EnableConfigurationProperties`. |
   | «¿Dónde pongo el valor por defecto en un `record`?» | No hay inicializadores de campo: `@DefaultValue` en el parámetro, o constructor compacto. |
   | La validación no salta | Falta `@Validated` en el `record`, o falta `spring-boot-starter-validation`. |

---

## Paso 4 · Perfiles y logging estructurado (EJ 2.2 y 2.3) · 25 min

1. **✏️ Proyecta los tres ficheros a la vez** (usa la vista dividida del IDE):
   [`application.yml`](src/main/resources/application.yml),
   [`application-dev.yml`](src/main/resources/application-dev.yml),
   [`application-prod.yml`](src/main/resources/application-prod.yml).

   **🗣️ La regla:** «El común arriba, lo que cambia por entorno abajo. Y fijaos en la línea 5 del común:
   `spring.profiles.default: dev`. No es lo mismo que `active`.»

2. **❓ Pregunta (y esta la respondes tú, porque casi nadie la sabe entera):**
   «¿Diferencia entre `spring.profiles.active`, `spring.profiles.default` y `spring.profiles.group`?»
   - `active`: lo fija quien despliega. Manda.
   - `default`: se usa **solo si nadie ha activado ninguno**. Por eso aquí, en local, estáis en `dev` sin saberlo.
   - `group`: activa varios de golpe. `prod` → `prod,metricas,nube`.

3. **⌨️ Arranca en `dev`** (en la terminal del proyecto; déjalo corriendo para el paso 5):

   ```bash
   ./mvnw -q -pl m2-boot spring-boot:run
   ```

   **✏️ Señala en el log:** `The following 1 profile is active: "dev"`, el `show-sql` de Hibernate creando
   las tablas y la consola H2 en <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:testdb`).

4. **🗣️ Explica el perfil `prod` sin arrancarlo todavía** (líneas 3-15 de `application-prod.yml`):
   PostgreSQL, y `spring.docker.compose.enabled: true`. «Con `spring-boot-docker-compose` en el classpath,
   **Boot arranca el `compose.yaml` él solo** y configura usuario, contraseña y URL a partir del servicio.
   No hay que copiar credenciales a ningún sitio.»

   **⌨️ Si tienes tiempo y Docker a mano** (2 min, pero es vistoso):

   ```bash
   ./mvnw -q -pl m2-boot spring-boot:run -Dspring-boot.run.profiles=prod
   ```

   Verás en el log cómo levanta el contenedor de PostgreSQL antes de crear el `DataSource`.

5. **EJ 2.3 · Logging estructurado.** **✏️ Señala las líneas 18-25** de `application-prod.yml`:
   `logging.structured.format.console: ecs`. **🗣️ Di:** «En `prod` cada línea de log es un JSON con
   `service.name`, `log.level`, `trace.id`... Es lo que espera Elastic. Hay también `logstash` y `gelf`.
   Antes de Boot 3.4 esto eran cien líneas de `logback-spring.xml`.»

   **❓ Pregunta:** «¿Por qué no en `dev`?» → Porque un humano no lee JSON. El formato del log es
   configuración de entorno, como la base de datos.

---

## Paso 5 · Actuator y métricas (EJ 2.4) · 35 min

Con la aplicación del paso 4 todavía corriendo en `dev`.

1. **⌨️ En la terminal libre, uno por uno** (proyecta la salida de cada uno antes de pasar al siguiente):

   ```bash
   curl -s localhost:8080/actuator/health
   ```

   ```json
   {"status":"UP","groups":["liveness","readiness"],"components":{
     "db":{"status":"UP","details":{"database":"H2","validationQuery":"isValid()"}},
     "diskSpace":{"status":"UP", ...},
     "livenessState":{"status":"UP"},"ping":{"status":"UP"},"readinessState":{"status":"UP"},
     "salas":{"status":"UP","details":{"salasConfiguradas":3}}, ...}}
   ```

   **🗣️ Señala `"salas"`:** «Ese componente lo hemos escrito nosotros. Los demás vienen de serie: la base de
   datos, el disco, las sondas.»

2. **⌨️ Crea una reserva y observa la cabecera `Location`:**

   ```bash
   curl -s -i -X POST localhost:8080/api/reservas -H 'Content-Type: application/json' \
     -d '{"sala":"Turing","usuario":"ana@atech.es","fecha":"2030-01-15","inicio":"09:00","fin":"11:00"}'
   ```

   ```
   HTTP/1.1 201
   Location: http://localhost:8080/api/reservas/1
   {"id":1,"sala":"Turing","usuario":"ana@atech.es","fecha":"2030-01-15","inicio":"09:00:00","fin":"11:00:00"}
   ```

3. **⌨️ Ahora el endpoint propio y la métrica:**

   ```bash
   curl -s localhost:8080/actuator/reservas
   # {"porSala":{"Turing":1,"Lovelace":0,"Hopper":0},"total":1}

   curl -s localhost:8080/actuator/metrics/atech.reservas.creadas
   # {"name":"atech.reservas.creadas", ... "measurements":[{"statistic":"COUNT","value":1.0}] ...}
   ```

   **🗣️ Di:** «Acabáis de ver el ciclo completo: una operación de negocio, un contador de Micrometer y el
   valor consultable. Eso mismo, con el endpoint `/actuator/prometheus`, es lo que raspa Prometheus y lo que
   acaba en un panel de Grafana.»

4. **✏️ Proyecta las tres clases del paquete `operacion`, 20 segundos cada una:**
   - [`SalasHealthIndicator`](src/main/java/com/atech/curso/m2/operacion/SalasHealthIndicator.java):
     `implements HealthIndicator`, y el nombre del bean (`@Component("salas")`) es el que aparece en el JSON.
   - [`CursoInfoContributor`](src/main/java/com/atech/curso/m2/operacion/CursoInfoContributor.java): cuatro líneas.
   - [`ReservasEndpoint`](src/main/java/com/atech/curso/m2/operacion/ReservasEndpoint.java):
     `@Endpoint(id = "reservas")` + `@ReadOperation`.

5. **✏️ Y el contador, en [`ReservaService` líneas 32-34](src/main/java/com/atech/curso/m2/reservas/ReservaService.java#L32-L34):**
   se construye **una vez** en el constructor y se incrementa en la línea 65. **🗣️ Aviso:** «Construir el
   contador dentro del método también funciona, pero es trabajo inútil en cada llamada.»

6. **Ejercicio (20 min):** el indicador de salud, el `InfoContributor`, el endpoint propio y el contador.
   Criterio:

   ```bash
   ./mvnw -pl m2-boot test -Dtest=ReservaApiTest#actuatorExponeSaludSondasYEndpointPropio
   ```

7. **❓ Mientras trabajan, pregunta al aire:** «¿Por qué `management.endpoints.web.exposure.include`
   ([líneas 26-30 de `application.yml`](src/main/resources/application.yml#L26-L30)) tiene que enumerarlos?»
   → Porque por defecto solo se expone `health`. Exponer `env` o `heapdump` sin pensarlo es un incidente de
   seguridad. En producción, Actuator va **en otro puerto**, cerrado al exterior (`management.server.port`).

---

## Paso 6 · Hilos virtuales e imagen OCI (EJ 2.5) · 15 min

**Lanza primero la construcción de la imagen y explica mientras compila.** Tarda varios minutos.

1. **⌨️ Arranca esto y déjalo corriendo en una terminal** (requiere Docker):

   ```bash
   ./mvnw -pl m2-boot spring-boot:build-image
   ```

2. **Mientras compila, los hilos virtuales.** **✏️ Señala
   [`application.yml` líneas 6-9](src/main/resources/application.yml#L6-L9):**
   `spring.threads.virtual.enabled: true`. Tres líneas de YAML.

3. **⌨️ Demuéstralo con una petición que falle** (con la aplicación del paso 4 aún corriendo):

   ```bash
   curl -s -X POST localhost:8080/api/reservas -H 'Content-Type: application/json' \
     -d '{"sala":"Babbage","usuario":"ana@atech.es","fecha":"2030-01-15","inicio":"09:00","fin":"10:00"}'
   ```

   **✏️ Proyecta el final de la traza que devuelve el JSON** y señala las dos últimas líneas:

   ```
   at com.atech.curso.m2.reservas.ReservaService$$SpringCGLIB$$0.crear(<generated>)
   ...
   at java.base/java.lang.VirtualThread.run(VirtualThread.java:329)
   ```

   **🗣️ Dos cosas de golpe:** «Abajo del todo, `VirtualThread.run`: esa petición se ha atendido en un hilo
   virtual, y lo único que hemos hecho es poner tres líneas de YAML. Y un poco más arriba,
   `ReservaService$$SpringCGLIB$$0`: **ahí está el proxy del módulo 1**, esta vez el de `@Transactional`.»

   **🗣️ Añade el matiz honesto:** «Los hilos virtuales ganan cuando el cuello de botella es esperar
   (llamadas HTTP, base de datos), no cuando es calcular. Y cuidado con `synchronized` y con los
   `ThreadLocal` en bucles: ahí todavía hay aristas.»

4. **Cuando termine la construcción, ⌨️ ejecuta la imagen:**

   ```bash
   docker run --rm -p 8080:8080 atech/m2-boot:1.0.0-SNAPSHOT
   ```

   **🗣️ Di:** «Ni un `Dockerfile`. Buildpacks detecta que es una aplicación Java, elige la JRE, separa las
   dependencias de vuestro código en capas distintas —así un cambio en vuestro código no reconstruye 200 MB—
   y firma la imagen. Si vuestra empresa tiene un `Dockerfile` de treinta líneas copiado de un blog de 2019,
   esto merece una conversación.»

   **↩️ Para el contenedor con `Ctrl+C`.**

---

## Paso 7 · Autoconfiguración propia (EJ 2.6) · 25 min

**Objetivo:** desmontar la palabra «magia». Es el cierre conceptual del módulo.

1. **✏️ Proyecta [`RelojAutoConfiguration.java`](src/main/java/com/atech/curso/m2/autoconfig/RelojAutoConfiguration.java)**
   entera (23 líneas) y léela en voz alta:

   ```java
   @AutoConfiguration
   public class RelojAutoConfiguration {
       @Bean
       @ConditionalOnMissingBean
       Clock clock() { return Clock.system(ZoneId.of("Europe/Madrid")); }
   }
   ```

2. **✏️ Y ahora el fichero que la registra:**
   [`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`](src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports)
   — **una línea, el nombre de la clase**.

   **🗣️ Di:** «Esto es todo. Una clase con condiciones y una línea en un fichero de texto. Los 150 *starters*
   de Spring Boot son exactamente esto, repetido. No hay nada más.»

3. **⌨️ Ejecuta el test y proyéctalo:**

   ```bash
   ./mvnw -pl m2-boot test -Dtest=RelojAutoConfigurationTest
   ```

   **✏️ Enseña los dos métodos**
   ([`RelojAutoConfigurationTest.java` líneas 21-31](src/test/java/com/atech/curso/m2/RelojAutoConfigurationTest.java#L21-L31)):
   sin `Clock` propio, la autoconfiguración lo pone; con `Clock` propio, **se aparta**.

   **🗣️ Di:** «`@ConditionalOnMissingBean` es la regla de oro de Boot: **lo tuyo siempre gana**. Es lo que
   hace que un framework con opinión no sea una jaula.»

4. **⌨️ El informe de autoconfiguración** (el momento «ajá» del módulo):

   ```bash
   ./mvnw -q -pl m2-boot spring-boot:run -Dspring-boot.run.arguments=--debug
   ```

   Busca en la salida `CONDITIONS EVALUATION REPORT` y luego, dentro, `RelojAutoConfiguration` y
   `DataSourceAutoConfiguration`. **🗣️ Di:** «Cada línea dice qué se aplicó y **por qué**, y las de
   `Negative matches` dicen qué **no** se aplicó y qué faltaba. La próxima vez que Boot haga algo que no
   entendéis, este informe os lo cuenta en diez segundos.»

5. **Ejercicio o comentario (según tiempo, 10 min):** extraerla a un módulo
   `atech-reloj-spring-boot-starter` con su propio `pom.xml`. **🗣️ Si no da tiempo**, cuenta al menos la
   convención de nombres: `xxx-spring-boot-starter` para los de terceros; `spring-boot-starter-xxx` está
   reservado a los oficiales.

---

## Paso 8 · Cierre del módulo · 10 min

1. **⌨️ Que todos ejecuten:**

   ```bash
   ./mvnw -pl m2-boot test
   ```

   Mínimo exigible: `ReservasPropertiesTest` y `RelojAutoConfigurationTest`.

2. **↩️ Limpieza antes de seguir** (importante: el módulo 3 usa el mismo puerto y la misma base de datos):

   ```bash
   docker compose -f m2-boot/compose.yaml down
   git status --short
   ```

3. **🗣️ Las tres frases del módulo:**
   - Boot no es magia: es `@Conditional`, un fichero de `imports` y valores por defecto sensatos.
   - La configuración es código: tiene tipos, se valida y mata el arranque si está mal.
   - Lo que no se puede medir en producción no se puede operar.

4. **Enlaza con el módulo 3:** «Hoy hemos guardado reservas con `ddl-auto: create-drop` y H2, que está muy
   bien para una demo y es inaceptable en producción. Mañana: entidades de verdad, consultas de verdad,
   transacciones y migraciones.»

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 6 completo | 15 min | Cuenta los hilos virtuales en una frase; la imagen OCI, en otra. |
| Paso 7, punto 5 (el *starter*) | 10 min | Queda como lectura del README. |
| Paso 5, ejercicio | 20 min | Conviértelo en demo: los `curl` ya están hechos, proyecta las tres clases. |

**No recortes el paso 1** (el arranque que falla) ni el punto 4 del paso 7 (el informe de autoconfiguración):
son los dos momentos que cambian cómo miran Spring Boot.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa | Comprobación |
|---|---|---|
| Las propiedades están a `null` | Falta `@ConfigurationPropertiesScan`/`@EnableConfigurationProperties` | Que el bean exista en el contexto |
| La validación no salta | Falta `@Validated` o el starter de validación | Prueba con `hasFailed()` en un `ApplicationContextRunner` |
| `/actuator/xxx` devuelve 404 | No está en `exposure.include` | `curl localhost:8080/actuator` lista los expuestos |
| El perfil `prod` no levanta PostgreSQL | Docker parado, o `spring.docker.compose.enabled: false` heredado del común | Log de arranque: busca `docker compose` |
| El puerto 8080 está ocupado | Ha quedado una demo anterior viva | `docker compose down` y cierra las terminales del módulo anterior |
