# Módulo 2 · Spring Boot a fondo

**Objetivo:** construir la API básica de **reservas de salas** con Spring Boot 3.5.x, prestando atención a la configuración, los perfiles, la observabilidad y el despliegue.

```bash
mvn -pl m2-boot spring-boot:run                                      # perfil dev (H2)
mvn -pl m2-boot spring-boot:run -Dspring-boot.run.profiles=prod      # PostgreSQL con Docker Compose
mvn -pl m2-boot spring-boot:build-image                              # imagen OCI con Buildpacks
curl -X POST localhost:8080/api/reservas -H 'Content-Type: application/json' \
     -d '{"sala":"Turing","usuario":"ana@atech.es","fecha":"2030-01-15","inicio":"09:00","fin":"11:00"}'
```

## Enunciados

### EJ 2.1 · Propiedades tipadas con `@ConfigurationProperties`
1. Crea el record `ReservasProperties` con el prefijo `atech.reservas`. Debe incluir: `salas` (lista obligatoria), `duracionMaxima` (`Duration`, por defecto `4h`), `horaApertura` y `horaCierre` (validadas con `@Min`/`@Max`) y un objeto anidado `notificaciones`.
2. Valídalo con `@Validated`; la aplicación **no debe arrancar** si la configuración es incorrecta.
3. Añade `spring-boot-configuration-processor` y comprueba el autocompletado en el IDE.
4. Pruébalo con `ApplicationContextRunner`, sin levantar la aplicación completa.

*Solución:* `config/ReservasProperties` · *Test:* `ReservasPropertiesTest`.

### EJ 2.2 · Perfiles
- Perfil `dev` (activo por defecto con `spring.profiles.default`): H2 en memoria, consola H2 y `show-sql`.
- Perfil `prod`: PostgreSQL. Con `spring-boot-docker-compose`, Boot arranca `compose.yaml` y configura la conexión automáticamente.
- *Para pensar:* ¿qué diferencia hay entre `spring.profiles.active`, `spring.profiles.default` y `spring.profiles.group`?

### EJ 2.3 · Logging estructurado
Configura `logging.structured.format.console: ecs` en `prod` y observa el JSON. Prueba también `logstash` y `gelf`.

### EJ 2.4 · Actuator y métricas
1. Expón `health`, `info`, `metrics`, `prometheus` y un endpoint propio.
2. Activa las sondas `liveness` y `readiness`.
3. Crea un `HealthIndicator` propio (`salas`), un `InfoContributor` y un endpoint `@Endpoint(id = "reservas")`.
4. Publica el contador `atech.reservas.creadas` con Micrometer y consúltalo en `/actuator/metrics/atech.reservas.creadas`.

*Solución:* paquete `operacion`, `ReservaService` · *Test:* `ReservaApiTest#actuatorExponeSaludSondasYEndpointPropio`.

### EJ 2.5 · Hilos virtuales e imagen OCI
1. Activa `spring.threads.virtual.enabled` y compara el tiempo de arranque y el consumo de memoria con y sin hilos virtuales.
2. Genera la imagen con `spring-boot:build-image` y ejecútala con `docker run -p 8080:8080 atech/m2-boot:1.0.0-SNAPSHOT`.

### EJ 2.6 · Autoconfiguración propia
1. Crea `RelojAutoConfiguration` con un `Clock` anotado con `@ConditionalOnMissingBean`.
2. Regístrala en `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
3. Prueba que cede el paso cuando la aplicación define su propio `Clock`.
4. *Ampliación:* extráela a un módulo `atech-reloj-spring-boot-starter` con su propio `pom.xml`.

*Test:* `RelojAutoConfigurationTest`.

## Extra Spring Boot 4 (rama `boot4`)
- Starters renombrados: `spring-boot-starter-webmvc`, y tests por tecnología (`spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`...).
- `@SpringBootTest` ya no configura MockMvc por sí solo: añade `@AutoConfigureMockMvc`, algo que este proyecto ya hace.
- Jackson 3: paquetes `tools.jackson.*`; `ObjectMapper` pasa a ser `JsonMapper`.
- Paquetes de Actuator reorganizados (`org.springframework.boot.health.contributor.*`).
- Compara el arranque en 3.5 y en 4.x con `-Dspring.main.lazy-initialization=true` y con hilos virtuales.
