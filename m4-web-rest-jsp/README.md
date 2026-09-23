# Módulo 4 · Spring MVC: vistas JSP y API REST

**Objetivo:** desarrollar la interfaz web de gestión de salas con **JSP + JSTL 3.0** (empaquetado WAR) y una **API REST** de reservas documentada con OpenAPI y probada con MockMvc y REST Assured.

```bash
./mvnw -pl m4-web-rest-jsp spring-boot:run
# http://localhost:8080/salas          (JSP)
# http://localhost:8080/swagger-ui.html (API)
./mvnw -pl m4-web-rest-jsp package && java -jar m4-web-rest-jsp/target/m4-web-rest-jsp-1.0.0-SNAPSHOT.war
```

## Enunciados

### EJ 4.1 · Vistas JSP con formularios
1. Configura JSP: empaquetado `war`, `tomcat-embed-jasper`, la API y la implementación de JSTL 3.0, y `spring.mvc.view.prefix/suffix`.
2. `lista.jsp`: tabla de salas con `c:forEach`, `c:choose`, `fmt:formatNumber` y un mensaje *flash* tras guardar. Usa los URI `jakarta.tags.*`.
3. `formulario.jsp`: alta y edición con las etiquetas `form:` de Spring, mostrando los errores de validación junto a cada campo.
4. Aplica el patrón **Post/Redirect/Get** con `RedirectAttributes` y valida también que el nombre no esté repetido (`rejectValue`).
5. Personaliza los mensajes en `messages.properties` (`NotBlank.sala.nombre`...).

*Solución:* paquete `web`, `src/main/webapp/WEB-INF/jsp` · *Test:* `SalaWebControllerTest`.

### EJ 4.2 · API REST y gestión de errores
1. CRUD en `/api/v1/reservas` con `ResponseEntity`: `201 Created` + `Location`, `204 No Content` al borrar y `404` si no existe.
2. Usa DTOs (`ReservaRequest`/`ReservaResponse`); las entidades no salen del servicio (`open-in-view: false`).
3. Errores en formato **`ProblemDetail`** (RFC 9457) con `@RestControllerAdvice` que extiende `ResponseEntityExceptionHandler`: 404, 422 (regla de negocio) y 400 con el detalle `errores` por campo.

### EJ 4.3 · Contrato y paginación
1. Documenta la API con springdoc (`@Tag`, `@Operation`, `@ApiResponse`, `@Schema`) y revisa Swagger UI.
2. Paginación y ordenación con `Pageable` (`?page=0&size=10&sort=inicio,desc`), devolviendo un `PagedModel` con una estructura JSON estable.

### EJ 4.4 · Pruebas
- `@WebMvcTest` + `@MockitoBean` para los controladores MVC y REST, estos últimos con REST Assured (`spring-mock-mvc`).
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` + REST Assured: ciclo de vida completo, contrato `/v3/api-docs` y renderizado real de la JSP.

## Extra Spring Boot 4
- **Versionado de API** nativo:
  ```yaml
  spring.mvc.apiversion.use.header: API-Version
  spring.mvc.apiversion.default: 1.0
  ```
  ```java
  @GetMapping(path = "/{id}", version = "1.0")  ReservaResponse buscarV1(...)
  @GetMapping(path = "/{id}", version = "2.0+") ReservaResponseV2 buscarV2(...)
  ```
- Pruebas con **`RestTestClient`** (`@AutoConfigureRestTestClient`), que sustituye a `TestRestTemplate`.
- springdoc **3.x** para Boot 4. JSP sigue soportado (Servlet 6.1, Tomcat 11).
