# Migración de los ejercicios a Spring Boot 4.x

Guía para la práctica «rama `boot4`»: crear una rama y migrar los módulos de 3.5.x a 4.x (Spring Framework 7, Jakarta EE 11, Hibernate 7, Jackson 3, Spring Security 7).

```bash
git switch -c boot4
```

## 0. Antes de empezar (en 3.5.x)

1. Deja la rama `main` sin avisos de *deprecación* (`./mvnw -Dmaven.compiler.showDeprecation=true compile`). En Boot 4 se eliminan las APIs que ya estaban deprecadas en 3.x.
2. Asegúrate de que todos los tests pasan (`./mvnw verify`).
3. Opcional: ejecuta **OpenRewrite**, que automatiza gran parte del trabajo:
   ```bash
   ./mvnw -U org.openrewrite.maven:rewrite-maven-plugin:run \
     -Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:RELEASE \
     -Drewrite.activeRecipes=org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0
   ```

## 1. `pom.xml` raíz

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.1</version>   <!-- última 4.x disponible -->
</parent>
<properties>
    <java.version>21</java.version>
    <springdoc.version>3.0.0</springdoc.version>  <!-- línea 3.x para Boot 4 -->
</properties>
```

## 2. Starters por módulo

| Módulo | Boot 3.5 | Boot 4 |
|---|---|---|
| m1 | `spring-boot-starter-aop` + `spring-retry` | `spring-boot-starter-aspectj` (sin `spring-retry`) |
| m2, m4, m5, m6, m7 | `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| m3 | `flyway-core` | `spring-boot-starter-flyway` (+ `flyway-database-postgresql`) |
| m7 | `spring-boot-starter-oauth2-client` / `-resource-server` | `spring-boot-starter-security-oauth2-client` / `-resource-server` |
| tests | `spring-boot-starter-test` | además, el starter de test de cada tecnología: `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-security-test`... |

> Si se quiere avanzar poco a poco, `spring-boot-starter-classic` y `spring-boot-starter-test-classic` mantienen el *classpath* monolítico de Boot 3.

## 3. Cambios de código

### m1 · Núcleo y AOP
- `@EnableRetry` y `org.springframework.retry.annotation.Retryable` pasan a ser `@EnableResilientMethods` y `org.springframework.resilience.annotation.Retryable` (atributos `includes`, `maxRetries`, `delay`, `multiplier`, `maxDelay`, `jitter`).
- Añade `package-info.java` con `@NullMarked` (JSpecify).

### m2 · Boot
- Los tests `@SpringBootTest` que usan `MockMvc` necesitan `@AutoConfigureMockMvc`, algo que ya está así.
- Paquetes de test movidos: `org.springframework.boot.test.autoconfigure.web.servlet.*` pasa a `org.springframework.boot.webmvc.test.autoconfigure.*`.
- Actuator: `HealthIndicator`/`Health` pasan a `org.springframework.boot.health.contributor.*`.
- Jackson 3: `com.fasterxml.jackson.databind` pasa a `tools.jackson.databind`. Las anotaciones `com.fasterxml.jackson.annotation` se mantienen.

### m3 · Persistencia
- `@DataJpaTest` pasa a `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, y `@AutoConfigureTestDatabase` a `org.springframework.boot.jdbc.test.autoconfigure`.
- Hibernate 7: revisa las consultas JPQL con literales de *enum* y las comparaciones `count(r) > 0`.

### m4 · Web
- `@WebMvcTest` pasa a `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- Versionado nativo de la API (sustituye `/api/v1` por una cabecera):
  ```yaml
  spring:
    mvc:
      apiversion:
        use:
          header: API-Version
        default: 1.0
  ```
- `TestRestTemplate` se sustituye por `RestTestClient` (`@AutoConfigureRestTestClient`).
- JSP: sin cambios funcionales (Servlet 6.1 y Tomcat 11). JSTL sigue siendo la 3.0.

### m5 · AMQP
- `Jackson2JsonMessageConverter` pasa a `JacksonJsonMessageConverter` (Jackson 3).
- Opcional: `spring-rabbitmq-client` + `RabbitAmqpTemplate` (AMQP 1.0, RabbitMQ 4).

### m6 · Integration
- `RequestHandlerRetryAdvice` y la nueva API de reintentos del núcleo (`org.springframework.core.retry.RetryTemplate`).
- `RestTemplateBuilder` pasa a `org.springframework.boot.restclient.RestTemplateBuilder`. Mejor aún, migra la pasarela HTTP a `RestClient`.

### m7 · Security
- El DSL con lambdas es obligatorio y `and()` ha desaparecido. El proyecto ya usa lambdas.
- `GitHubClient` se puede sustituir por una interfaz `@HttpExchange` + `@ClientRegistrationId("github")` + `@ImportHttpServices` (ver el README del módulo).
- Prueba MFA (`@EnableMultiFactorAuthentication`) y *passkeys* (`http.webAuthn(..)`).

## 4. Verificación

```bash
./mvnw -U clean verify
```

Para la práctica del módulo 2, compara el tiempo de arranque y la memoria en 3.5 y en 4.x, con y sin `spring.threads.virtual.enabled`.
