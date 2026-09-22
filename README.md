# Spring Boot Avanzado · Ejercicios prácticos

Proyecto de ejercicios del curso **Spring Boot Avanzado** (Atech Advanced Solutions).
Hay un módulo Maven por cada tema del curso, y todos giran en torno al mismo dominio: una aplicación de **reservas de salas**.

| Versión base | Notas |
|---|---|
| **Spring Boot 3.5.x** (Spring Framework 6.2, Jakarta EE 10, Hibernate 6.6, Jackson 2) | Rama `main` |
| **Spring Boot 4.x** (Spring Framework 7, Jakarta EE 11, Hibernate 7, Jackson 3) | Guía paso a paso en [docs/MIGRACION-BOOT4.md](docs/MIGRACION-BOOT4.md) y apartados «Extra Spring Boot 4» de cada módulo |

## Módulos

| Módulo | Tema | Qué se practica |
|---|---|---|
| [`m1-core-aop`](m1-core-aop) | Núcleo de Spring y AOP | IoC/DI por constructor, `@Profile`, `@Primary`/`@Qualifier`, eventos, `@Aspect`, auto-invocación, reintentos |
| [`m2-boot`](m2-boot) | Spring Boot | Starters, `@ConfigurationProperties` con records, perfiles, logging estructurado, Actuator, autoconfiguración propia, Docker Compose, imágenes OCI |
| [`m3-persistencia`](m3-persistencia) | Spring Data JPA | Entidades y relaciones, `@Embeddable` record, consultas derivadas, JPQL, proyecciones, Specifications, Flyway, transacciones, bloqueo optimista, N+1, Testcontainers |
| [`m4-web-rest-jsp`](m4-web-rest-jsp) | Spring MVC | JSP + JSTL 3.0 + formularios, API REST, `ProblemDetail`, paginación, springdoc/OpenAPI, `@WebMvcTest`, REST Assured |
| [`m5-amqp`](m5-amqp) | RabbitMQ | Exchanges/colas/DLQ, JSON, consumidores idempotentes, reintentos, `RepublishMessageRecoverer`, publisher confirms, Testcontainers |
| [`m6-integration`](m6-integration) | Spring Integration | `@MessagingGateway`, JDBC poller, filtro, splitter, router, agregador, HTTP y AMQP salientes, `errorChannel`, `MockIntegrationContext` |
| [`m7-security-oauth2`](m7-security-oauth2) | Spring Security | Varias `SecurityFilterChain`, login social OIDC/OAuth 2.0, alta automática, Resource Server JWT (Keycloak), roles, seguridad de método, `RestClient` + OAuth 2.0, tests |

Cada módulo contiene:

- `README.md` con los **enunciados** (EJ n.m), pistas y la forma de comprobar la solución.
- La **solución de referencia** en `src/main`. Las clases indican con comentarios `EJ n.m` a qué ejercicio corresponden.
- **Tests** que validan cada ejercicio (`src/test`).
- `compose.yaml` con la infraestructura necesaria (PostgreSQL, RabbitMQ, Keycloak) cuando aplica.

> **Cómo usarlo en clase.** Se recomienda que el alumnado cree su propio proyecto con [Spring Initializr](https://start.spring.io) siguiendo el enunciado y consulte este repositorio para comparar. Otra opción es partir de una rama sin las clases de solución y usar los tests de este repositorio como criterio de aceptación.

## Requisitos

- JDK 21 (compatible con 17–25 en Boot 3.5; Boot 4 exige 17+)
- Maven 3.9+
- Docker (para `compose.yaml` y los tests con Testcontainers). **Sin Docker, esos tests se omiten** (`@Testcontainers(disabledWithoutDocker = true)`) y el resto funciona con H2.
- IDE recomendado: IntelliJ IDEA, VS Code con Spring Boot Extension Pack o Spring Tools.

## Compilar y probar

```bash
mvn verify                       # todos los módulos
mvn -pl m3-persistencia verify   # un módulo
mvn -pl m2-boot spring-boot:run  # ejecutar un módulo
```

El workflow de GitHub Actions (`.github/workflows/ci.yml`) ejecuta `mvn verify` con JDK 21 en cada *push*. Los runners de GitHub tienen Docker, así que allí también se ejecutan los tests de Testcontainers.

## Puertos y servicios

| Servicio | Módulo | URL |
|---|---|---|
| Aplicación | todos | http://localhost:8080 |
| Consola H2 | m2 | http://localhost:8080/h2-console |
| Swagger UI | m4 | http://localhost:8080/swagger-ui.html |
| RabbitMQ (gestión) | m5, m6 | http://localhost:15672 (guest/guest) |
| Keycloak | m7 | http://localhost:8180 (admin/admin) |

## Licencia

Material docente de uso interno para el curso. © Atech Advanced Solutions.
