# Guión de clase · Módulo 4 · Spring MVC: vistas JSP y API REST

**Duración:** 3 h 15 min netas, **repartidas en dos sesiones** · Enunciados en [README.md](README.md)

- **Final de la sesión 3 (1 h 15):** pasos 1 a 4 — las vistas JSP y los formularios.
- **Sesión 4 (2 h):** pasos 5 a 9 — la API REST, los errores, el contrato y las pruebas.

> **Iconos:** **⌨️** ejecuta · **✏️** edita o proyecta · **🗣️** dilo así · **❓** pregunta al aula ·
> **🔴** rotura provocada · **↩️** deshacer. Comandos **desde la raíz del repositorio**; en Windows, `mvnw.cmd`.
>
> **Aviso de tono.** En cuanto digas «JSP» alguien preguntará si eso no está muerto. **Respóndelo tú el
> primero, en dos minutos y sin ironía:** hay muchísimo JSP en producción y alguien tiene que mantenerlo;
> el objetivo real del bloque es el **modelo de Spring MVC** (`Model`, *binding*, validación,
> `BindingResult`, redirecciones), que es idéntico con Thymeleaf. Dicho eso, sigue adelante y no vuelvas a
> disculparte.

---

## Paso 0 · Antes de entrar en el aula (10 min)

```bash
./mvnw -pl m4-web-rest-jsp test        # 3 clases en verde
./mvnw -q -pl m4-web-rest-jsp spring-boot:run
```

Con la aplicación levantada, **comprueba las tres URL y déjalas en pestañas del navegador**:

- <http://localhost:8080/salas> — el listado JSP
- <http://localhost:8080/swagger-ui.html> — Swagger UI
- <http://localhost:8080/v3/api-docs> — el contrato en JSON

Pestañas del IDE:

1. [`src/main/webapp/WEB-INF/jsp/salas/lista.jsp`](src/main/webapp/WEB-INF/jsp/salas/lista.jsp)
2. [`src/main/webapp/WEB-INF/jsp/salas/formulario.jsp`](src/main/webapp/WEB-INF/jsp/salas/formulario.jsp)
3. [`src/main/java/com/atech/curso/m4/web/SalaWebController.java`](src/main/java/com/atech/curso/m4/web/SalaWebController.java)
4. [`src/main/java/com/atech/curso/m4/api/ReservaRestController.java`](src/main/java/com/atech/curso/m4/api/ReservaRestController.java)
5. [`src/main/java/com/atech/curso/m4/api/ErroresApiHandler.java`](src/main/java/com/atech/curso/m4/api/ErroresApiHandler.java)

---

## Cronograma

| Paso | Contenido | Min | Sesión |
|---|---|---|---|
| 1 | Gancho: la web y la API, la misma aplicación | 10 | 3 |
| 2 | Concepto: MVC y configuración de JSP | 15 | 3 |
| 3 | Ejercicio EJ 4.1 (vistas y formularios) | 35 | 3 |
| 4 | Puesta en común + 🔴 Post/Redirect/Get | 15 | 3 |
| 5 | Concepto: API REST y errores (EJ 4.2) | 20 | 4 |
| 6 | Ejercicio EJ 4.2 | 40 | 4 |
| 7 | Contrato y paginación (EJ 4.3) | 30 | 4 |
| 8 | Pruebas (EJ 4.4) | 20 | 4 |
| 9 | Cierre del módulo | 10 | 4 |

---

## Paso 1 · Gancho: la web y la API, la misma aplicación · 10 min

1. **⌨️ Arranca la aplicación y déjala corriendo todo el bloque:**

   ```bash
   ./mvnw -q -pl m4-web-rest-jsp spring-boot:run
   ```

2. **✏️ Enseña las dos caras, en el navegador, sin explicar todavía:**
   - <http://localhost:8080/salas>: una página HTML con su tabla y su formulario.
   - <http://localhost:8080/swagger-ui.html>: la API, con sus operaciones y su botón «Try it out».

3. **🗣️ Di:** «Mismo proyecto, mismo servicio, mismo dominio. Lo único que cambia es **quién está al otro
   lado**: una persona con un navegador, o un programa. Hoy vamos a ver las dos y, sobre todo, en qué se
   parecen: las dos son un controlador que traduce HTTP a una llamada al servicio.»

4. **❓ Pregunta para situar al grupo:** «¿Cuántos tenéis aplicaciones con JSP en producción? ¿Y cuántos
   servís HTML desde Spring y no desde un frontal en React o Angular?» Ajusta el énfasis del paso 3 según lo
   que salga: si casi nadie usa JSP, acorta el ejercicio y pasa antes al bloque REST.

---

## Paso 2 · Concepto: MVC y configuración de JSP · 15 min

1. **Dibuja el flujo en la pizarra** (te sirve también para el bloque REST):

   ```
   petición ──► DispatcherServlet ──► controlador ──► Model ──► ViewResolver ──► vista (.jsp)
                                           │
                                           └──► @ResponseBody / @RestController ──► JSON
   ```

   **🗣️ Di:** «La única diferencia entre una web y una API en Spring MVC es la última flecha.»

2. **✏️ La configuración de JSP, en dos sitios y nada más:**
   - [`pom.xml`](pom.xml): `<packaging>war</packaging>`, `tomcat-embed-jasper`, y la API **y** la
     implementación de JSTL 3.0.
   - [`application.yml` líneas 4-8](src/main/resources/application.yml#L4-L8):
     `spring.mvc.view.prefix: /WEB-INF/jsp/` y `suffix: .jsp`.

   **🗣️ Explica el `/WEB-INF/`:** «Las JSP van ahí porque **el contenedor no las sirve directamente**. Solo
   se puede llegar a ellas pasando por un controlador. Si las dejáis fuera, cualquiera puede pedirlas por
   URL y saltarse vuestra lógica.»

3. **✏️ Proyecta [`lista.jsp` líneas 1-5](src/main/webapp/WEB-INF/jsp/salas/lista.jsp#L1-L5)** y señala el
   detalle que hace perder más tiempo:

   ```jsp
   <%@ taglib prefix="c" uri="jakarta.tags.core" %>
   ```

   **🗣️ Di:** «`jakarta.tags.core`, no `http://java.sun.com/jsp/jstl/core`. Con Jakarta EE 10 cambiaron
   todos los URI. **Todos los ejemplos que encontréis en internet están con el URI antiguo** y fallan con un
   error que no dice nada útil. Apuntadlo.»

4. **✏️ Recorre el cuerpo de la vista** (30 segundos cada cosa): `c:if` con el mensaje *flash* (línea 16),
   `c:choose`/`c:when` para la lista vacía (20-24), `c:forEach` con `varStatus` (30-31),
   `fmt:formatNumber` (33) y `c:url` (11, 35), que añade el contexto de la aplicación.

---

## Paso 3 · Ejercicio EJ 4.1 (vistas y formularios) · 35 min

1. **✏️ Antes de soltarlos, proyecta
   [`formulario.jsp` líneas 16-29](src/main/webapp/WEB-INF/jsp/salas/formulario.jsp#L16-L29)** y explica las
   etiquetas `form:` de Spring:

   ```jsp
   <form:form modelAttribute="sala" method="post" action="...">
       <form:input path="nombre" cssErrorClass="error"/>
       <form:errors path="nombre" cssClass="error"/>
   ```

   **🗣️ Di:** «`path="nombre"` enlaza en los dos sentidos: rellena el campo con el valor del objeto y, al
   enviar, escribe en él. Y `form:errors` pinta el error **junto a su campo**, no un mensaje genérico
   arriba. Esto es lo que se pierde cuando se escribe el formulario a mano en HTML.»

2. **✏️ Y el controlador**
   ([`SalaWebController.java` líneas 58-70](src/main/java/com/atech/curso/m4/web/SalaWebController.java#L58-L70)),
   señalando el orden de los parámetros:

   ```java
   public String guardar(@Valid @ModelAttribute("sala") SalaForm form, BindingResult errores,
           RedirectAttributes redirect) {
   ```

   **🗣️ Aviso que salva media hora:** «`BindingResult` **tiene que ir inmediatamente después** del objeto
   validado. Si metéis algo en medio, Spring lanza la excepción de validación en vez de dejaros tratarla, y
   el mensaje no os dirá que el problema es el orden.»

3. **Ejercicio (25 min).** El listado, el formulario de alta y edición, la validación con mensajes por campo
   y el mensaje *flash*.

   **⌨️ Criterio de aceptación:**

   ```bash
   ./mvnw -pl m4-web-rest-jsp test -Dtest=SalaWebControllerTest
   ```

4. **Mientras trabajan**, los cuatro atascos de siempre:

   | Síntoma | Causa |
   |---|---|
   | El navegador **descarga** la JSP en vez de mostrarla | Falta `tomcat-embed-jasper`, o el empaquetado no es `war` |
   | `The absolute uri [...] cannot be resolved` | URI antiguo de JSTL, o falta la **implementación** (no solo la API) |
   | Los errores no se muestran | Falta `BindingResult`, o está mal colocado |
   | Los mensajes salen en inglés y feos | Faltan las claves en [`messages.properties`](src/main/resources/messages.properties) |

---

## Paso 4 · Puesta en común + 🔴 Post/Redirect/Get · 15 min

1. **🔴 La demo del F5** (hazla en el navegador, en directo; dura un minuto y no se olvida):

   **✏️ En [`SalaWebController.java` línea 69](src/main/java/com/atech/curso/m4/web/SalaWebController.java#L69)**,
   cambia la redirección por una vista directa:

   ```java
   return VISTA_LISTA;        // antes: return "redirect:/salas";
   ```

   **⌨️ Reinicia, crea una sala en el formulario y pulsa F5.** El navegador pregunta si quieres reenviar el
   formulario; si dices que sí, **se crea otra sala**.

   **🗣️ Di:** «Esto es un pedido duplicado, un cobro duplicado. El patrón se llama **Post/Redirect/Get** y
   existe justo para esto: después de un POST, redirige; así el F5 repite un GET, que es inofensivo.»

   **↩️ Deshaz:**

   ```bash
   git checkout -- m4-web-rest-jsp/src/main/java/com/atech/curso/m4/web/SalaWebController.java
   ```

2. **❓ Pregunta encadenada:** «Si redirijo, el objeto del modelo se pierde. ¿Cómo enseño entonces el mensaje
   *Sala guardada*?» → Con `RedirectAttributes.addFlashAttribute` (línea 68): Spring lo guarda en la sesión,
   lo pone en el modelo de la siguiente petición y **lo borra**. De ahí el nombre.

3. **✏️ Y la validación de negocio**
   ([líneas 61-63](src/main/java/com/atech/curso/m4/web/SalaWebController.java#L61-L63)):

   ```java
   errores.rejectValue("nombre", "sala.nombre.duplicado", "Ya existe una sala con ese nombre");
   ```

   **🗣️ La distinción importante:** «`@NotBlank` es formato: se comprueba sin saber nada del mundo.
   *Que no exista otra sala con ese nombre* es una **regla de negocio**: hace falta ir a la base de datos.
   La primera va en el DTO; la segunda, en el controlador o el servicio, con `rejectValue`, para que el
   error salga **en el campo** y no como una pantalla de error.»

   **⌨️ Enséñalo probado:**

   ```bash
   ./mvnw -pl m4-web-rest-jsp test -Dtest=SalaWebControllerTest#nombreDuplicadoEsUnErrorDeCampo
   ```

4. **🗣️ Cierra la sesión 3** con el puente: «Mañana, la misma funcionalidad para un cliente que no es una
   persona. Y veremos que la mitad de las decisiones son las mismas y la otra mitad, muy distintas.»

---

## Paso 5 · Concepto: API REST y errores (EJ 4.2) · 20 min

1. **⌨️ Arranca otra vez la aplicación** (la necesitas todo el bloque):

   ```bash
   ./mvnw -q -pl m4-web-rest-jsp spring-boot:run
   ```

2. **✏️ Proyecta [`ReservaRestController.java`](src/main/java/com/atech/curso/m4/api/ReservaRestController.java)**
   y recorre los códigos de estado, que es donde está el contenido:

   | Líneas | Qué señalar |
   |---|---|
   | 61-70 | `201 Created` **con cabecera `Location`**. «El cliente no debería tener que adivinar la URL del recurso que acaba de crear.» |
   | 78-84 | `204 No Content` al borrar: sin cuerpo. |
   | 53-59 | El `404` no se escribe aquí: lo lanza el servicio y lo traduce el *advice*. |
   | 35 | `/api/v1/...`: la versión, en la URI. En Boot 4 hay versionado nativo por cabecera (ver README). |

3. **🗣️ DTOs, y por qué:** [`ReservaRequest`](src/main/java/com/atech/curso/m4/api/ReservaRequest.java) y
   [`ReservaResponse`](src/main/java/com/atech/curso/m4/api/ReservaResponse.java). «Las entidades **no salen
   del servicio**. Tres razones: no queréis que un `@Column` renombrado rompa el contrato de vuestros
   clientes; no queréis exponer campos internos; y con `open-in-view: false` serializar una entidad con
   relaciones `LAZY` revienta. Fijaos en que esto último es exactamente el N+1 de ayer, disfrazado.»

4. **⌨️ Ahora los errores, en directo, uno por uno.** Proyecta la salida de cada `curl`:

   ```bash
   curl -s -i localhost:8080/api/v1/reservas/999 | head -2
   ```

   ```
   HTTP/1.1 404
   Content-Type: application/problem+json
   ```

   ```bash
   curl -s localhost:8080/api/v1/reservas/999
   ```

   ```json
   {"type":"https://api.atech.es/problemas/no-encontrado","title":"Recurso no encontrado",
    "status":404,"detail":"Reserva con id 999 no encontrado","instance":"/api/v1/reservas/999"}
   ```

   ```bash
   curl -s -X POST localhost:8080/api/v1/reservas -H 'Content-Type: application/json' -d '{"usuario":""}'
   ```

   ```json
   {"type":"https://api.atech.es/problemas/validacion","title":"Datos no válidos","status":400,
    "detail":"Invalid request content.","instance":"/api/v1/reservas",
    "errores":{"usuario":"no debe estar vacío","salaId":"no debe ser nulo",
               "inicio":"no debe ser nulo","fin":"no debe ser nulo"}}
   ```

   ```bash
   curl -s -X POST localhost:8080/api/v1/reservas -H 'Content-Type: application/json' \
     -d '{"salaId":1,"usuario":"ana@atech.es","inicio":"2030-02-01T12:00:00","fin":"2030-02-01T11:00:00"}'
   ```

   ```json
   {"type":"https://api.atech.es/problemas/regla-negocio","title":"Regla de negocio incumplida",
    "status":422,"detail":"La fecha de fin debe ser posterior a la de inicio","instance":"/api/v1/reservas"}
   ```

5. **🗣️ Remata:** «Esto es la RFC 9457, `ProblemDetail`. Tipo, título, estado, detalle e instancia, con
   `Content-Type: application/problem+json`. **Es un estándar**: un cliente puede tratar los errores de
   cualquier API que lo use sin leerse vuestra documentación.»

   **❓ Pregunta:** «¿Cuántos formatos de error distintos hay hoy en vuestra empresa?» → Normalmente uno por
   equipo, y ninguno documentado. Ese es el argumento.

6. **❓ El debate de los 3 minutos: 400 frente a 422.** Déjalos discutir y no des una respuesta tajante:
   `400` = no te entiendo (sintaxis, tipos, campos obligatorios); `422` = te entiendo perfectamente, pero no
   puedo hacerlo (la sala está ocupada, el fin es anterior al inicio). **No hay consenso en la industria**;
   lo que no vale es devolver `500`, ni `200` con un campo `error` dentro.

7. **✏️ Y el mecanismo, para cerrar**
   ([`ErroresApiHandler.java` líneas 26-27](src/main/java/com/atech/curso/m4/api/ErroresApiHandler.java#L26-L27)):

   ```java
   @RestControllerAdvice(basePackageClasses = ReservaRestController.class)
   public class ErroresApiHandler extends ResponseEntityExceptionHandler {
   ```

   **🗣️ Dos detalles finos:** el `basePackageClasses` limita el *advice* **a la API**, para que las vistas
   JSP sigan con su página de error normal; y al extender `ResponseEntityExceptionHandler` se heredan los
   errores estándar de Spring MVC y solo hay que enriquecer el de validación (líneas 48-60, con
   `pd.setProperty("errores", ...)`).

---

## Paso 6 · Ejercicio EJ 4.2 · 40 min

1. **🗣️ Enunciado:** CRUD en `/api/v1/reservas` con `ResponseEntity`, DTOs de entrada y salida, y los tres
   errores en `ProblemDetail` (404, 422 y 400 con el detalle por campo).

   **⌨️ Criterio de aceptación:**

   ```bash
   ./mvnw -pl m4-web-rest-jsp test -Dtest=ReservaRestControllerTest
   ```

2. **Pistas que puedes dar si se atascan** (en este orden, no todas de golpe):
   - Para el `Location`: `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id)`.
   - Para el 404: que el **servicio** lance `RecursoNoEncontradoException`; el controlador no debe saber de
     códigos HTTP para eso.
   - `@Valid` en el `@RequestBody`, o la validación no se dispara.

3. **❓ Pregunta para la puesta en común:** «¿Dónde habéis puesto la traducción de excepción a código HTTP?»
   → Debe estar en **un solo sitio** (el *advice*). Si aparece un `try/catch` con `ResponseEntity.status(404)`
   dentro de un controlador, es el momento de decir que eso acaba repetido en veinte sitios y divergiendo.

---

## Paso 7 · Contrato y paginación (EJ 4.3) · 30 min

1. **✏️ Abre <http://localhost:8080/swagger-ui.html> y usa «Try it out» en directo:** crea una reserva desde
   la propia página. **🗣️ Di:** «Esto no es documentación que alguien escribió y se quedó vieja: sale del
   código. Si mañana cambiáis el DTO, cambia sola.»

2. **✏️ Enseña de dónde salen los textos** en el controlador: `@Tag` (línea 36), `@Operation` (47, 54, 62),
   `@ApiResponse` (55-56, 63-64) y `@Parameter` (57). Y
   [`OpenApiConfig`](src/main/java/com/atech/curso/m4/api/OpenApiConfig.java) para los datos generales.

3. **⌨️ El contrato en crudo** (es lo que consumen los generadores de clientes):

   ```bash
   curl -s localhost:8080/v3/api-docs | head -c 300
   ```

   ```json
   {"openapi":"3.1.0","info":{"title":"API de Reservas","description":"Curso Desarrollo de aplicaciones Spring - Atech...
   ```

4. **Paginación. ⌨️ Ejecuta y proyecta:**

   ```bash
   curl -s "localhost:8080/api/v1/reservas?page=0&size=2&sort=inicio,desc"
   ```

   ```json
   {"content":[],"page":{"size":2,"number":0,"totalElements":0,"totalPages":0}}
   ```

   **✏️ Señala [la línea 50 del controlador](src/main/java/com/atech/curso/m4/api/ReservaRestController.java#L50):**
   `return new PagedModel<>(servicio.listar(pagina));`

   **🗣️ Explica por qué `PagedModel` y no `Page`:** «Serializar un `Page` directamente funciona, pero su
   JSON es **inestable**: depende de la implementación interna de Spring Data y ya ha cambiado entre
   versiones, rompiendo clientes. `PagedModel` os da la estructura estable `content` + `page`. Spring Data
   avisa de esto por log desde hace varias versiones y casi nadie lo lee.»

5. **✏️ Y el límite:** [`application.yml`](src/main/resources/application.yml),
   `spring.data.web.pageable.max-page-size: 100`.
   **❓ Pregunta:** «¿Qué pasa si un cliente pide `?size=1000000`?» → Sin ese límite, se lo lleva todo a
   memoria. Es una denegación de servicio en una línea de YAML.

6. **Ejercicio corto (15 min):** documentar dos operaciones y devolver el listado paginado.

---

## Paso 8 · Pruebas (EJ 4.4) · 20 min

1. **✏️ Proyecta las dos cabeceras, una al lado de la otra**, y explica la diferencia (es lo más útil del
   bloque):

   ```java
   @WebMvcTest(ReservaRestController.class)     // ReservaRestControllerTest
   class ReservaRestControllerTest {
       @MockitoBean ReservaService servicio;     // el servicio es un doble
   ```

   ```java
   @SpringBootTest(webEnvironment = RANDOM_PORT) // ReservaApiIntegrationTest
   class ReservaApiIntegrationTest {
       @LocalServerPort int puerto;              // Tomcat de verdad
   ```

   | | `@WebMvcTest` | `@SpringBootTest(RANDOM_PORT)` |
   |---|---|---|
   | Qué levanta | Solo la capa web | La aplicación entera, con Tomcat |
   | Servicio | `@MockitoBean` | El real, con base de datos |
   | Velocidad | Milisegundos | Segundos |
   | Para qué | Códigos, cabeceras, JSON, validación | Que todo encaje de verdad |

2. **⌨️ Ejecuta el de integración y proyéctalo:**

   ```bash
   ./mvnw -pl m4-web-rest-jsp test -Dtest=ReservaApiIntegrationTest
   ```

3. **✏️ Enseña `cicloDeVidaCompletoDeUnaReserva`**: crea (201) → **usa la cabecera `Location` devuelta** para
   leer (200) → lista paginada → borra (204) → vuelve a leer (404).
   **🗣️ Di:** «Fijaos en que el test **navega por la propia API**: usa el `Location` en vez de construir la
   URL a mano. Si mañana cambia la ruta, el test sigue valiendo.»

4. **✏️ Y los otros dos tests del mismo fichero, que son los que menos se hacen y más valen:**
   - `publicaElContratoOpenApi`: **el contrato está probado**. El día que alguien rompa la API sin querer,
     falla el build.
   - `laVistaJspSeRenderiza`: la JSP se renderiza de verdad, con Tomcat, no con un *mock*.

5. **🗣️ Cierra con el criterio práctico:** «Muchos `@WebMvcTest` (baratos, precisos) y unos pocos de
   integración que recorran el camino completo. Si todos vuestros tests son de integración, el build tarda
   veinte minutos y nadie lo ejecuta antes de subir.»

---

## Paso 9 · Cierre del módulo · 10 min

1. **⌨️ Que todos ejecuten:**

   ```bash
   ./mvnw -pl m4-web-rest-jsp test
   ```

   Mínimo exigible: `SalaWebControllerTest` y `ReservaRestControllerTest`.

2. **↩️ Limpieza:** para la aplicación (`Ctrl+C`) y comprueba `git status --short`.

3. **🗣️ Las tres frases del módulo:**
   - El controlador traduce HTTP a una llamada al servicio, y nada más.
   - Los errores son parte del contrato: mismo formato, en un solo sitio, y probados.
   - Una API sin contrato publicado es una API que nadie sabe usar sin preguntaros.

4. **Enlaza con el módulo 5:** «Hasta ahora, alguien pide algo y espera la respuesta. Mañana: alguien publica
   un hecho y se va, y otro lo procesa cuando puede. Cambian las garantías, los errores y la forma de
   probarlo.»

---

## Anexo A · Si vas con retraso

| Recorte | Ganas | Cómo |
|---|---|---|
| Paso 7, ejercicio | 15 min | Enseña Swagger UI funcionando y deja documentar para casa. |
| Paso 3, edición de salas | 10 min | Que hagan solo el alta; la edición queda de ejercicio. |
| Paso 8 | 10 min | Ejecuta los dos tests y proyecta la tabla comparativa, sin escribir código. |

**No recortes el paso 4** (Post/Redirect/Get) **ni el 5** (`ProblemDetail`): son los dos que se llevan al
trabajo al día siguiente.

## Anexo B · Errores frecuentes y respuesta rápida

| Síntoma | Causa |
|---|---|
| La JSP se descarga en vez de mostrarse | Empaquetado `jar` en vez de `war`, o falta `tomcat-embed-jasper` |
| `The absolute uri [...] cannot be resolved` | URI de JSTL antiguo (`java.sun.com`) o falta la implementación |
| La validación no se dispara en la API | Falta `@Valid` en el `@RequestBody` |
| Los errores del formulario no aparecen | `BindingResult` ausente o no colocado justo detrás del objeto |
| `@WebMvcTest` falla al arrancar por beans que faltan | Solo carga la capa web: lo demás, con `@MockitoBean` |
| `LazyInitializationException` al serializar | Se está devolviendo la entidad en vez del DTO (`open-in-view: false`) |
| Las tildes salen mal en los errores de la API | `application/problem+json` no declara charset; en los tests se fuerza UTF-8 (ver `ReservaRestControllerTest`) |
