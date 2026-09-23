# Módulo 1 · El núcleo de Spring y AOP

**Objetivo:** modelar un pequeño servicio de pedidos y notificaciones usando el contenedor IoC, la inyección de dependencias, los perfiles, los eventos de aplicación y la programación orientada a aspectos.

```bash
./mvnw -pl m1-core-aop spring-boot:run                                   # notificador por email
./mvnw -pl m1-core-aop spring-boot:run -Dspring-boot.run.profiles=sms    # notificador por SMS
./mvnw -pl m1-core-aop test
```

## Enunciados

### EJ 1.1 · Varias implementaciones seleccionadas por perfil
1. Crea la interfaz `Notificador` con los métodos `canal()` y `notificar(destinatario, mensaje)`.
2. Implementa `EmailNotificador` (activo **salvo** con el perfil `sms`) y `SmsNotificador` (activo **sólo** con el perfil `sms`, trunca a 160 caracteres).
3. Comprueba que el código cliente no cambia al cambiar de perfil.

*Pista:* `@Profile("!sms")` admite expresiones (`!`, `&`, `|`).
*Solución:* paquete `notificacion` · *Tests:* `PerfilPorDefectoTest`, `PerfilSmsTest`.

### EJ 1.2 · Inyección por constructor y desambiguación
1. Crea `PedidoService` con **inyección por constructor** (campos `final`, sin `@Autowired`).
2. Crea la interfaz `CalculadoraDescuento` con dos implementaciones: `SinDescuento` marcada con `@Primary` y `DescuentoBlackFriday` con `@Qualifier("blackFriday")`.
3. Define un bean `Clock` e inyéctalo para poder hacer pruebas deterministas.
4. Escribe un test unitario **sin Spring** que construya el servicio con `new`.

*Solución:* `PedidoService`, paquete `precios`, `M1Application#clock` · *Test:* `PedidoServiceUnitTest`.

### EJ 1.3 · Eventos de aplicación
1. Al confirmar un pedido, publica el evento `PedidoConfirmado` (un `record`) con `ApplicationEventPublisher`.
2. Crea `NotificacionPedidoListener` con `@EventListener` que envíe la notificación. `PedidoService` no debe conocer al `Notificador`.
3. Crea otro listener que sólo reaccione a pedidos de más de 1000 € usando `condition` (SpEL).
4. Pruébalo con `@RecordApplicationEvents`.

*Para pensar:* ¿qué ocurre si el listener lanza una excepción? ¿Y con `@TransactionalEventListener`?

### EJ 1.4 · Aspecto de auditoría
1. Crea la anotación `@Auditado(String value)`.
2. Implementa `AuditoriaAspect` con un consejo `@Around("@annotation(auditado)")` que mida el tiempo y registre si la llamada terminó bien o con excepción.
3. Anota `PedidoService#confirmar` y comprueba que se audita tanto el éxito como el fallo.

*Solución:* paquete `auditoria` · *Test:* `PedidoServiceTest`.

### EJ 1.5 · El problema de la auto-invocación
1. Añade `confirmarLote(List<Pedido>)`, que llama internamente a `this.confirmar(..)`.
2. Demuestra con un test que **el aspecto no se aplica** a esas llamadas internas, aunque los eventos sí se publican.
3. Propón al menos dos soluciones y aplica una de ellas en tu proyecto.

### EJ 1.6 · Reintentos declarativos
1. `EmailNotificador` simula un servidor SMTP inestable (`simularFallos(n)`).
2. Añade `@Retryable` (Spring Retry) con 3 intentos y *backoff* exponencial, y activa los reintentos con `@EnableRetry`.
3. Comprueba que, con 2 fallos simulados, el mensaje se envía en el tercer intento.

## Extra Spring Boot 4

- Cambia `spring-boot-starter-aop` por **`spring-boot-starter-aspectj`**.
- Sustituye Spring Retry por la resiliencia del núcleo de Spring Framework 7:
  ```java
  @Configuration @EnableResilientMethods
  class ResilienciaConfig { }

  @org.springframework.resilience.annotation.Retryable(
          includes = IllegalStateException.class, maxRetries = 2, delay = 50, multiplier = 2)
  public void notificar(String destinatario, String mensaje) { ... }
  ```
  Prueba también `@ConcurrencyLimit(1)` en `SmsNotificador`.
- Añade un `package-info.java` con `@NullMarked` (JSpecify) en `com.atech.curso.m1` y marca con `@Nullable` lo que pueda ser nulo. Activa NullAway en la compilación para ver los avisos.
- Registra beans programáticamente con la nueva interfaz `BeanRegistrar` (por ejemplo, un `Notificador` por cada canal configurado).
